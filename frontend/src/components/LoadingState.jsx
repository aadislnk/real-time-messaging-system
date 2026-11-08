export default function LoadingState({ label = 'Loading…' }) {
  return <div className="state loading-state" role="status"><span className="spinner" />{label}</div>
}
