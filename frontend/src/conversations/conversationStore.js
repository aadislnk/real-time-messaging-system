export function findDirectConversation(conversations, currentUserId, otherUserId) {
  return conversations.find((conversation) => {
    if (conversation.type !== 'DIRECT') return false
    const memberIds = new Set((conversation.members || []).map((member) => member.userId))
    return memberIds.size === 2 && memberIds.has(currentUserId) && memberIds.has(otherUserId)
  })
}

export function upsertConversation(conversations, conversation) {
  if (!conversation?.id) return conversations
  return [conversation, ...conversations.filter((item) => item.id !== conversation.id)]
}

export function dedupeConversations(conversations) {
  const seen = new Set()
  return conversations.filter((conversation) => {
    if (!conversation?.id || seen.has(conversation.id)) return false
    seen.add(conversation.id)
    return true
  })
}
