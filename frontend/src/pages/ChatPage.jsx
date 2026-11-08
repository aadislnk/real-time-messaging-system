import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/AppShell'
import Button from '../components/Button'
import Dialog from '../components/Dialog'
import EmptyState from '../components/EmptyState'
import ErrorState from '../components/ErrorState'
import LoadingState from '../components/LoadingState'
import ConnectionStatus from '../components/ConnectionStatus'
import { useAuth } from '../auth/AuthProvider'
import { errorMessage } from '../api/apiError'
import { listConversations, getConversation, createConversation, addMember, removeMember, renameConversation, deleteConversation } from '../conversations/conversationApi'
import { dedupeConversations, findDirectConversation, upsertConversation } from '../conversations/conversationStore'
import { searchUsers } from '../users/userApi'
import { acknowledge, sendMessage, syncMessages } from '../messages/messageApi'
import { createClientMessageId } from '../messages/clientMessageId'
import { mergeMessages, messageStatus } from '../messages/messageStore'
import { searchMessages } from '../search/searchApi'
import { createRealtimeClient } from '../realtime/stompClient'
import { eventPayload } from '../realtime/realtimeEvents'
import TypingIndicator from '../typing/TypingIndicator'

export default function ChatPage() {
  const { token, user, logout } = useAuth(); const navigate = useNavigate()
  const [conversations, setConversations] = useState([]); const [activeId, setActiveId] = useState(null); const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(true); const [messageLoading, setMessageLoading] = useState(false); const [error, setError] = useState(null); const [connection, setConnection] = useState('connecting')
  const [typingName, setTypingName] = useState(''); const [dialog, setDialog] = useState(null); const [toast, setToast] = useState('')
  const realtime = useRef(null); const activeRef = useRef(activeId); const messagesRef = useRef(messages); const typingTimer = useRef(null); const acknowledged = useRef(new Set()); const syncRequest = useRef(0)
  activeRef.current = activeId; messagesRef.current = messages
  const active = conversations.find((item) => item.id === activeId)

  const refreshConversations = useCallback(async (selectFirst = true) => {
    try {
      const data = dedupeConversations(await listConversations(token)); setConversations(data); setError(null)
      if (selectFirst && !activeRef.current && data[0]) setActiveId(data[0].id)
      if (activeRef.current && !data.some((item) => item.id === activeRef.current)) { setActiveId(null); setMessages([]) }
      return data
    } catch (e) { setError(e); throw e } finally { setLoading(false) }
  }, [token])

  const sync = useCallback(async (id = activeRef.current) => {
    if (!id) return
    const requestId = ++syncRequest.current
    setMessageLoading(true)
    try {
      const cursor = messagesRef.current.reduce((max, message) => Math.max(max, message.sequenceNumber || 0), 0)
      const page = await syncMessages(token, id, cursor || 0)
      if (activeRef.current !== id || syncRequest.current !== requestId) return page
      setMessages((current) => mergeMessages(current, page.messages || []))
      return page
    } catch (e) {
      if (activeRef.current === id && syncRequest.current === requestId) {
        setError(e); setToast(errorMessage(e))
      }
    } finally {
      if (syncRequest.current === requestId) setMessageLoading(false)
    }
  }, [token])

  useEffect(() => { refreshConversations().catch(() => {}) }, [refreshConversations])
  useEffect(() => {
    if (!activeId) return undefined
    setMessages([]); acknowledged.current = new Set()
    getConversation(token, activeId).then((detail) => setConversations((items) => items.map((item) => item.id === detail.id ? detail : item))).catch(() => {})
    sync(activeId)
    return () => { syncRequest.current += 1; setMessages([]); setTypingName('') }
  }, [activeId, sync, token])
  useEffect(() => {
    if (!activeId) return
    messages.filter((message) => message.id && message.senderId !== user.id && !acknowledged.current.has(message.id)).forEach((message) => {
      acknowledged.current.add(message.id)
      acknowledge(token, activeId, message.id, 'READ').catch(() => {})
    })
  }, [messages, activeId, token, user.id])
  useEffect(() => {
    const client = createRealtimeClient({
      token, onStatus: setConnection, onReconnect: () => { refreshConversations(false); sync() },
      onEvent: (event) => {
        const payload = eventPayload(event)
        if (event.type === 'MESSAGE_CREATED' && payload) {
          setConversations((items) => items.map((item) => item.id === event.conversationId ? { ...item, latestMessagePreview: payload.content } : item))
          if (event.conversationId === activeRef.current) setMessages((current) => mergeMessages(current, [payload]))
        } else if (event.type === 'MESSAGE_DELIVERED' || event.type === 'MESSAGE_READ') {
          setMessages((current) => current.map((message) => message.id === payload?.messageId && message.senderId === user.id ? { ...message, receiptState: payload.receiptState } : message))
        } else if (event.type === 'CONVERSATION_CREATED' || event.type === 'CONVERSATION_UPDATED') {
          setConversations((items) => upsertConversation(items, payload))
        } else if (event.type === 'CONVERSATION_REMOVED') {
          refreshConversations(false)
        } else if (event.type === 'TYPING_STARTED') {
          if (event.conversationId === activeRef.current && event.actorUserId !== user.id) { setTypingName(payload?.actorDisplayName || 'Someone'); clearTimeout(typingTimer.current); typingTimer.current = setTimeout(() => setTypingName(''), (payload?.expiresInSeconds || 5) * 1000) }
        } else if (event.type === 'TYPING_STOPPED' && event.conversationId === activeRef.current) setTypingName('')
      }
    })
    realtime.current = client; client.activate()
    return () => { client.disconnect(); clearTimeout(typingTimer.current) }
  }, [token, user.id, refreshConversations, sync])

  async function send(content, existingId) {
    if (!activeId || !content.trim()) return
    const clientMessageId = existingId || createClientMessageId(); const clean = content.trim()
    if (!existingId) setMessages((items) => [...items, { id: `optimistic-${clientMessageId}`, conversationId: activeId, senderId: user.id, clientMessageId, content: clean, createdAt: new Date().toISOString(), optimistic: true, status: 'sending' }])
    try {
      const response = await sendMessage(token, activeId, { clientMessageId, content: clean })
      setMessages((items) => mergeMessages(items, [{ ...response, optimistic: false }]))
      setConversations((items) => items.map((item) => item.id === activeId ? { ...item, latestMessagePreview: response.content } : item))
    } catch (e) {
      setMessages((items) => items.map((message) => message.clientMessageId === clientMessageId ? { ...message, optimistic: true, status: 'failed', error: errorMessage(e) } : message))
    }
  }
  function typing(yes) { realtime.current?.sendTyping(activeId, yes) }

  return <AppShell conversations={conversations} activeId={activeId} onSelect={setActiveId} onNew={() => setDialog('create')} onSearch={() => setDialog('search')} loading={loading} connection={connection} onLogout={() => { realtime.current?.disconnect(); logout(); navigate('/login', { replace: true }) }} overlays={<>
    <CreateDialog open={dialog === 'create'} token={token} conversations={conversations} userId={user.id} onClose={() => setDialog(null)} onCreated={(item) => { setConversations((items) => upsertConversation(items, item)); setActiveId(item.id); setDialog(null) }} />
    <SearchDialog open={dialog === 'search'} token={token} conversations={conversations} onClose={() => setDialog(null)} onSelect={(id) => { setActiveId(id); setDialog(null) }} />
    <ManageDialog open={dialog === 'manage'} conversation={active} token={token} user={user} onClose={() => setDialog(null)} onUpdated={(item) => { setConversations((items) => items.map((x) => x.id === item.id ? item : x)); setDialog(null) }} onDeleted={() => { setDialog(null); refreshConversations(false) }} />
  </>}>
    <ConnectionStatus status={connection} />
    {error && !conversations.length ? <ErrorState error={error} retry={() => refreshConversations()} /> : !active ? <div className="welcome-panel"><h1>Good conversations<br /><em>start here.</em></h1><p>Choose a conversation or start something new.</p><Button onClick={() => setDialog('create')}>Start a conversation <span>→</span></Button></div> :
      <ChatView conversation={active} messages={messages} user={user} loading={messageLoading} onSend={send} onTyping={typing} typingName={typingName} onManage={() => setDialog('manage')} onBack={() => setActiveId(null)} />}
    {toast && <div className="toast" role="status">{toast}</div>}
  </AppShell>
}

function ChatView({ conversation, messages, user, loading, onSend, onTyping, typingName, onManage, onBack }) {
  const other = conversation.members?.find((member) => member.userId !== user.id); const title = conversation.type === 'GROUP' ? conversation.title : (other?.displayName || 'Direct chat')
  return <section className="chat-view"><header className="chat-header"><button className="icon-button back-button" aria-label="Back to conversations" onClick={onBack}>←</button><span className="avatar">{title.slice(0, 1).toUpperCase()}</span><div><h2>{title}</h2>{conversation.type === 'GROUP' && <small>{conversation.members?.length || 0} members</small>}</div>{conversation.type === 'GROUP' && <button className="icon-button header-action" aria-label="Conversation settings" onClick={onManage}>•••</button>}</header>
    <div className="timeline" aria-live="polite">{loading && !messages.length ? <LoadingState label="Loading messages…" /> : !messages.length ? <EmptyState title="No messages yet" icon="♡">Send the first message to get things started.</EmptyState> : messages.map((message) => message.type === 'SYSTEM' ? <div className="system-message" key={message.id || message.clientMessageId}>{message.content}</div> : <MessageBubble key={message.id || message.clientMessageId} message={message} own={message.senderId === user.id} userId={user.id} onRetry={() => onSend(message.content, message.clientMessageId)} />)}<TypingIndicator name={typingName} /></div>
    <Composer onSend={onSend} onTyping={onTyping} />
  </section>
}

function MessageBubble({ message, own, userId, onRetry }) {
  const status = messageStatus(message, userId)
  return <div className={`message-row ${own ? 'own' : ''}`}><div className="message-bubble"><p>{message.content}</p><small>{message.createdAt ? new Date(message.createdAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }) : ''}{own && status && <span className={`message-status ${status}`}> · {status === 'failed' ? <button onClick={onRetry}>Retry</button> : status}</span>}</small></div></div>
}

function Composer({ onSend, onTyping }) {
  const [value, setValue] = useState(''); const timer = useRef(null)
  function submit(event) { event.preventDefault(); if (!value.trim()) return; onSend(value); setValue(''); onTyping(false) }
  function change(event) { setValue(event.target.value); onTyping(true); clearTimeout(timer.current); timer.current = setTimeout(() => onTyping(false), 1200) }
  return <form className="composer" onSubmit={submit}><textarea value={value} onChange={change} maxLength="4000" placeholder="Write a message…" aria-label="Message" rows="1" onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit(e) } }} /><span className="composer-count">{value.length}/4000</span><button className="send-button" aria-label="Send message" type="submit">↑</button></form>
}

function CreateDialog({ open, token, conversations, userId, onClose, onCreated }) {
  const [type, setType] = useState('DIRECT'); const [title, setTitle] = useState(''); const [query, setQuery] = useState(''); const [users, setUsers] = useState([]); const [selected, setSelected] = useState([]); const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  const selectedIds = useMemo(() => new Set(selected.map((item) => item.id)), [selected]); const userCache = useRef(new Map())
  useEffect(() => { if (!open) return; setType('DIRECT'); setTitle(''); setQuery(''); setUsers([]); setSelected([]); setError('') }, [open])
  useEffect(() => {
    if (!open) return undefined
    const cached = userCache.current.get(query)
    if (cached) { setUsers(cached); return undefined }
    const controller = new AbortController()
    const timer = setTimeout(() => searchUsers(token, query, controller.signal).then((data) => { userCache.current.set(query, data); setUsers(data) }).catch((e) => { if (e.name !== 'AbortError') setError(errorMessage(e)) }), 250)
    return () => { clearTimeout(timer); controller.abort() }
  }, [query, token, open])
  function toggle(item) { setSelected((items) => items.some((x) => x.id === item.id) ? items.filter((x) => x.id !== item.id) : [...items, item]) }
  async function submit(event) { event.preventDefault(); if ((type === 'DIRECT' && selected.length !== 1) || (type === 'GROUP' && selected.length < 2)) return setError(type === 'DIRECT' ? 'Choose one person.' : 'Choose at least two people.'); if (type === 'GROUP' && !title.trim()) return setError('Group title is required.'); setBusy(true); try { const existing = type === 'DIRECT' ? findDirectConversation(conversations, userId, selected[0].id) : undefined; onCreated(existing || await createConversation(token, { type, title: type === 'GROUP' ? title.trim() : undefined, memberIds: selected.map((x) => x.id) })) } catch (e) { setError(errorMessage(e)) } finally { setBusy(false) } }
  return <Dialog open={open} title="Start a conversation" onClose={onClose}><form className="dialog-form" onSubmit={submit}><div className="segmented"><button type="button" className={type === 'DIRECT' ? 'selected' : ''} onClick={() => setType('DIRECT')}>Direct</button><button type="button" className={type === 'GROUP' ? 'selected' : ''} onClick={() => setType('GROUP')}>Group</button></div>{type === 'GROUP' && <label>Group name<input value={title} maxLength="120" onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Weekend plans" /></label>}<label>Find people<input autoFocus value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search by name or email" /></label>{error && <div className="form-error">{error}</div>}<div className="user-results">{users.map((item) => <button type="button" key={item.id} className={`user-result ${selectedIds.has(item.id) ? 'selected' : ''}`} onClick={() => toggle(item)}><span className="avatar avatar-small">{item.displayName.slice(0, 1)}</span><span><strong>{item.displayName}</strong><small>{item.email}</small></span><span className="check">{selectedIds.has(item.id) ? '✓' : '+'}</span></button>)}</div><div className="dialog-actions"><Button type="button" variant="ghost" onClick={onClose}>Cancel</Button><Button disabled={busy}>{busy ? 'Creating…' : 'Create'}</Button></div></form></Dialog>
}

function SearchDialog({ open, token, conversations, onClose, onSelect }) {
  const [query, setQuery] = useState(''); const [results, setResults] = useState([]); const [busy, setBusy] = useState(false)
  useEffect(() => { if (!open) return undefined; setQuery(''); setResults([]) }, [open])
  useEffect(() => { if (!open || !query.trim()) return undefined; const timer = setTimeout(() => { setBusy(true); searchMessages(token, query.trim()).then(setResults).finally(() => setBusy(false)) }, 300); return () => clearTimeout(timer) }, [query, token, open])
  return <Dialog open={open} title="Search messages" onClose={onClose} wide><div className="dialog-form"><input autoFocus value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search across your conversations…" />{busy && <LoadingState label="Searching…" />}{!busy && query && !results.length && <EmptyState title="No matches" />}{results.map((result) => <button className="search-result" key={result.messageId} onClick={() => onSelect(result.conversationId)}><strong>{conversations.find((x) => x.id === result.conversationId)?.title || 'Conversation'}</strong><p>{result.content}</p><small>{new Date(result.createdAt).toLocaleString()}</small></button>)}</div></Dialog>
}

function ManageDialog({ open, conversation, token, user, onClose, onUpdated, onDeleted }) {
  const [title, setTitle] = useState(''); const [query, setQuery] = useState(''); const [users, setUsers] = useState([]); const [busy, setBusy] = useState(false); const [error, setError] = useState('')
  useEffect(() => { if (open) { setTitle(conversation?.title || ''); setQuery(''); setError('') } }, [open, conversation])
  useEffect(() => { if (!open || !query) return undefined; const timer = setTimeout(() => searchUsers(token, query).then(setUsers).catch(() => {}), 250); return () => clearTimeout(timer) }, [query, token, open])
  if (!conversation) return null
  const member = conversation.members?.find((item) => item.userId === user.id); const manager = member?.role === 'OWNER' || member?.role === 'ADMIN'; const owner = member?.role === 'OWNER'
  async function action(fn) { setBusy(true); try { onUpdated(await fn()) } catch (e) { setError(errorMessage(e)) } finally { setBusy(false) } }
  async function destructive(fn) { setBusy(true); try { await fn(); onDeleted() } catch (e) { setError(errorMessage(e)) } finally { setBusy(false) } }
  return <Dialog open={open} title="Conversation details" onClose={onClose}><div className="manage-panel">{error && <div className="form-error">{error}</div>}{conversation.type === 'GROUP' && manager && <><label>Group name<input value={title} onChange={(e) => setTitle(e.target.value)} maxLength="120" /></label><Button disabled={busy || !title.trim()} onClick={() => action(() => renameConversation(token, conversation.id, title.trim()))}>Save name</Button></>}{conversation.type === 'GROUP' && <><h3>Members <span>{conversation.members?.length}</span></h3><div className="member-list">{conversation.members?.map((item) => <div className="member-row" key={item.userId}><span className="avatar avatar-small">{item.displayName.slice(0, 1)}</span><span><strong>{item.displayName}</strong><small>{item.role.toLowerCase()}</small></span>{manager && item.userId !== user.id && item.role !== 'OWNER' && <button className="text-button danger" onClick={() => action(() => removeMember(token, conversation.id, item.userId))}>Remove</button>}</div>)}</div>{manager && <><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Add a member…" />{users.map((item) => <button className="user-result" key={item.id} onClick={() => action(() => addMember(token, conversation.id, { userId: item.id, role: 'MEMBER' }))}><span>{item.displayName}</span><span>＋</span></button>)}</>}</>}{conversation.type === 'GROUP' && owner && <Button variant="danger" disabled={busy} onClick={() => { if (window.confirm('Permanently delete this group for everyone?')) destructive(() => deleteConversation(token, conversation.id, true)) }}>Delete group</Button>}</div></Dialog>
}
