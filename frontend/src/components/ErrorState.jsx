import { errorMessage } from '../api/apiError'
export default function ErrorState({ error, retry }) {
  return <div className="state error-state" role="alert"><strong>Something went wrong</strong><p>{errorMessage(error)}</p>{retry && <button className="text-button" onClick={retry}>Try again</button>}</div>
}
