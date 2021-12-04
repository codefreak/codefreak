import { MutationFunction, useMutation, UseMutationOptions } from 'react-query'
import { messageService } from '../../services/message'
import { UseMutationResult } from 'react-query/types/react/types'

/**
 * Provides the option to disable the global error handling for the request
 */
interface UseWorkspaceBaseMutationOptions {
  /**
   * Option to disable the global error handling for the request
   */
  disableGlobalErrorHandling?: boolean
}

/**
 * Extends react-query's `useMutation` with global error handling
 *
 * @param mutationFn the mutation function
 * @param onError a custom callback for when an error occurs
 * @param otherOptions the other options for the mutation
 * @param disableGlobalErrorHandling whether to disable the global error handling
 */
const useWorkspaceBaseMutation = <
  TData = unknown,
  TError = unknown,
  TVariables = void,
  TContext = unknown
>(
  mutationFn: MutationFunction<TData, TVariables>,
  {
    onError,
    ...otherOptions
  }: Omit<
    UseMutationOptions<TData, TError, TVariables, TContext>,
    'mutationFn'
  > = {},
  { disableGlobalErrorHandling = false }: UseWorkspaceBaseMutationOptions = {}
): UseMutationResult<TData, TError, TVariables, TContext> =>
  useMutation(mutationFn, {
    ...otherOptions,
    onError: (error, variables, context) => {
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
        return onError(error, variables, context)
      }
    }
  })

export default useWorkspaceBaseMutation
