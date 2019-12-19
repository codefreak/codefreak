import { GraphQLError } from 'graphql'

export * from '../generated/graphql'

export const extractErrorMessage = (error: {
  graphQLErrors?: ReadonlyArray<GraphQLError>
}) => {
  return (error.graphQLErrors || []).map(e => e.message).join('\n')
}
