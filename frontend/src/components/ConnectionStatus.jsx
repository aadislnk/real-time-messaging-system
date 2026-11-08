export default function ConnectionStatus({ status }) {
  if (status === 'connected') return null
  return <div className="connection-banner" role="status">● {status === 'connecting' || status === 'reconnecting' ? 'Reconnecting to live updates…' : 'Realtime is unavailable. Messages still work.'}</div>
}
