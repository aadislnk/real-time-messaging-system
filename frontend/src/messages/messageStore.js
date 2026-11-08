export function mergeMessages(current, incoming) {
  const map = new Map(current.map((message) => [message.id || message.clientMessageId, message]))
  incoming.forEach((message) => {
    const key = message.id || message.clientMessageId
    const optimistic = [...map.values()].find((item) =>
      item.clientMessageId && message.clientMessageId && item.clientMessageId === message.clientMessageId)
    if (optimistic && optimistic !== message) map.delete(optimistic.id || optimistic.clientMessageId)
    map.set(key, { ...optimistic, ...message, optimistic: false, status: message.receiptState?.toLowerCase() || 'sent' })
  })
  return [...map.values()].sort((a, b) => (a.sequenceNumber || Number.MAX_SAFE_INTEGER) - (b.sequenceNumber || Number.MAX_SAFE_INTEGER))
}

export function messageStatus(message, currentUserId) {
  if (message.optimistic) return message.status || 'sending'
  if (message.senderId !== currentUserId) return null
  return (message.receiptState || 'SENT').toLowerCase()
}
