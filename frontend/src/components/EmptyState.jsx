export default function EmptyState({ title, children, icon = '✦' }) {
  return <div className="state empty-state"><span className="empty-icon">{icon}</span><strong>{title}</strong>{children && <p>{children}</p>}</div>
}
