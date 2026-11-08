import { describe, expect, it } from 'vitest'
import { dedupeConversations, findDirectConversation, upsertConversation } from './conversationStore'

const direct = {
  id: 'conversation-1',
  type: 'DIRECT',
  members: [{ userId: 'user-1' }, { userId: 'user-2' }]
}

describe('conversation identity helpers', () => {
  it('finds an existing direct conversation by stable member IDs', () => {
    expect(findDirectConversation([direct], 'user-1', 'user-2')).toBe(direct)
    expect(findDirectConversation([direct], 'user-1', 'different-user')).toBeUndefined()
  })

  it('upserts by conversation ID without adding duplicates', () => {
    const updated = { ...direct, latestMessagePreview: 'Updated' }
    expect(upsertConversation([direct], updated)).toEqual([updated])
  })

  it('deduplicates list entries by conversation ID', () => {
    expect(dedupeConversations([direct, direct, { id: 'conversation-2' }])).toEqual([direct, { id: 'conversation-2' }])
  })
})
