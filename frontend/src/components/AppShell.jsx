import { useState } from 'react'
import Button from './Button'
import { useAuth } from '../auth/AuthProvider'

export default function AppShell({ conversations, activeId, onSelect, onNew, onSearch, loading, children, overlays, connection, onLogout }) {
  const { user } = useAuth()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  return <div className={`app-shell ${activeId ? 'has-active-chat' : 'has-chat-list'}`}>
    <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`}>
      <div className="brand-row"><strong>Sandesh</strong><button className="icon-button mobile-only" aria-label="Close navigation" onClick={() => setSidebarOpen(false)}>×</button></div>
      <div className="sidebar-actions"><Button onClick={onNew}>＋ New chat</Button><button className="search-trigger" onClick={onSearch}>⌕ <span>Search chats</span><kbd>⌘ K</kbd></button></div>
      <div className="section-label"><span>Conversations</span><span>{conversations.length}</span></div>
      <div className="conversation-list" aria-label="Conversations">
        {loading ? <div className="list-loading">Loading conversations…</div> : conversations.map((conversation) => {
          const other = conversation.members?.find((member) => member.userId !== user.id)
          const name = conversation.type === 'GROUP' ? conversation.title : (other?.displayName || 'Direct chat')
          return <button key={conversation.id} className={`conversation-item ${activeId === conversation.id ? 'active' : ''}`} onClick={() => { onSelect(conversation.id); setSidebarOpen(false) }}>
            <span className="avatar">{(name || '?').slice(0, 1).toUpperCase()}</span><span className="conversation-copy"><strong>{name}</strong><small>{conversation.latestMessagePreview || 'No messages yet'}</small></span>
          </button>
        })}
        {!loading && !conversations.length && <div className="list-empty">No conversations yet.<br />Start a new chat.</div>}
      </div>
      <div className="profile-card"><span className="avatar avatar-small">{user.displayName.slice(0, 1).toUpperCase()}</span><span><strong>{user.displayName}</strong><small>{user.email}</small></span><button className="icon-button" aria-label="Sign out" onClick={onLogout}>↗</button></div>
    </aside>
    <main className="main-panel"><header className="mobile-header"><button className="icon-button" aria-label="Open navigation" onClick={() => setSidebarOpen(true)}>☰</button><strong>Sandesh</strong><span className={`connection-dot ${connection}`} title={connection}>{connection === 'connected' ? 'Live' : connection}</span></header>{children}</main>
    {overlays}
  </div>
}
