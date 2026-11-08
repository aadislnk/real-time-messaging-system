import { describe, expect, it } from 'vitest'
import { mergeMessages } from './messageStore'

describe('message reconciliation', () => {
  it('replaces an optimistic message with its idempotent REST response', () => {
    const optimistic = { id: 'local', clientMessageId: 'web-1', content: 'hello', optimistic: true }
    const server = { id: 'server-1', clientMessageId: 'web-1', content: 'hello', sequenceNumber: 4, receiptState: 'SENT' }
    const result = mergeMessages([optimistic], [server])
    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({ id: 'server-1', optimistic: false, sequenceNumber: 4 })
  })

  it('orders messages by server sequence rather than timestamp', () => {
    const result = mergeMessages([], [
      { id: 'b', sequenceNumber: 2 }, { id: 'a', sequenceNumber: 1 }
    ])
    expect(result.map((message) => message.id)).toEqual(['a', 'b'])
  })
})
