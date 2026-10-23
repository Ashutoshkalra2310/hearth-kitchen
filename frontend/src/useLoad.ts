import { useEffect, useState } from 'react'

/**
 * Loads data when the component mounts and again whenever `deps` change.
 * Responses that arrive after deps have changed are ignored, so a slow old request
 * can't overwrite a newer one.
 */
export function useLoad<T>(load: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T>()
  const [error, setError] = useState<string>()

  useEffect(() => {
    let current = true
    setError(undefined)
    load().then(
      (result) => current && setData(result),
      (err: Error) => current && setError(err.message),
    )
    return () => {
      current = false
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  return { data, setData, error }
}
