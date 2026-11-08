export default function TypingIndicator({ name }) {
  return name ? <div className="typing-indicator"><span className="typing-dots"><i /><i /><i /></span>{name} is typing…</div> : null
}
