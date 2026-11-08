export function eventPayload(event) {
  if (typeof event?.payload !== 'string') return event?.payload
  try { return JSON.parse(event.payload) } catch { return undefined }
}
