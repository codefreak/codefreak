import { useEffect, useState } from 'react'
import { fetchWithAuthentication } from '../../services/workspace'
import { messageService } from '../../services/message'
import { NO_AUTH_TOKEN, NO_BASE_URL } from './useWorkspace'
import useWorkspaceBaseQuery from './useWorkspaceBaseQuery'

/**
 * Checks whether the workspace with the given base-url is available.
 * Shows an error message to the user when the availability stops.
 * Automatically checks the availability every 10 seconds.
 *
 * @param baseUrl the base-url of the workspace
 * @param authToken the token to authenticate with the workspace. Can be an empty string if no token is required
 * @param pollingInterval the interval in which to check whether the workspace is still available. Set this to false to disable automatic checks
 */
const useIsWorkspaceAvailableQuery = (
  baseUrl: string,
  authToken?: string,
  pollingInterval: number | false = 10000
) => {
  const [isAvailable, setIsAvailable] = useState(false)

  const { data, isError } = useWorkspaceBaseQuery(
    ['isWorkspaceAvailable', baseUrl, authToken],
    () =>
      fetchWithAuthentication(baseUrl, {
        authToken: authToken ?? NO_AUTH_TOKEN
      }).then(() => Promise.resolve(true)),
    {
      enabled: baseUrl !== NO_BASE_URL && authToken !== undefined,
      // Retry three times on errors because the workspace might take some time to start
      retry: 3,
      // Fetch the result every ten seconds to see whether the workspace is still alive
      refetchInterval: pollingInterval
    }
  )

  useEffect(() => {
    if (baseUrl === NO_BASE_URL) {
      return
    }

    if (data !== undefined && data && !isError) {
      setIsAvailable(true)
    }

    if (isAvailable && isError) {
      messageService.error(
        'Workspace is not available anymore, trying to reconnect'
      )
      setIsAvailable(false)
    }
  }, [baseUrl, data, isAvailable, isError])

  return isAvailable
}

export default useIsWorkspaceAvailableQuery
