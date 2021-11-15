/**
 * Represents an error from graphql-ws
 */
export default interface GraphqlWsErrorType {
  /**
   * The error message (reason)
   */
  reason: string

  /**
   * The status code of the response
   */
  code: number
}
