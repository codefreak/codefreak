import { useQuery } from 'react-query'
import { QueryFunction, QueryKey } from 'react-query/types/core/types'
import { UseQueryOptions, UseQueryResult } from 'react-query/types/react/types'
import { messageService } from '../../services/message'

/**
 * Provides the option to disable the global error handling for the request
 */
interface UseWorkspaceBaseQueryOptions {
  /**
   * Option to disable the global error handling for the request
   */
  disableGlobalErrorHandling?: boolean
}

/**
 * Extends react-query's `useMutation` with global error handling
 *
 * @param queryKey the key to identify the query
 * @param queryFn the query function
 * @param onError a custom callback for when an error occurs
 * @param otherOptions the other options for the query
 * @param disableGlobalErrorHandling whether to disable the global error handling
 */
const useWorkspaceBaseQuery = <
  TQueryFnData = unknown,
  TError = unknown,
  TData = TQueryFnData,
  TQueryKey extends QueryKey = QueryKey
>(
  queryKey: TQueryKey,
  queryFn: QueryFunction<TQueryFnData, TQueryKey>,
  {
    onError,
    ...otherOptions
  }: Omit<
    UseQueryOptions<TQueryFnData, TError, TData, TQueryKey>,
    'queryKey' | 'queryFn'
  > = {},
  { disableGlobalErrorHandling = false }: UseWorkspaceBaseQueryOptions = {}
): UseQueryResult<TData, TError> =>
  useQuery(queryKey, queryFn, {
    ...otherOptions,
    onError: error => {
      if (!disableGlobalErrorHandling) {
        let message = 'An error occurred'

        if (error instanceof Error) {
          message = error.message
        } else if (typeof error === 'string') {
          message = error
        }

        messageService.error(message)
      }

      if (onError) {
        return onError(error)
      }
    }
  })

export default useWorkspaceBaseQuery
