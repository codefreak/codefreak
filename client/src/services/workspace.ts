import { NO_AUTH_TOKEN } from '../hooks/workspace/useWorkspace'
import {
  trimTrailingSlashes,
  withLeadingSlash,
  withTrailingSlash
} from './strings'
import { messageService } from './message'

/**
 * The api-route to be appended to the base-url when reading and creating files
 */
export const FILES_API_ROUTE = 'files'

/**
 * The api-route to be appended to the base-url when uploading file-contents
 */
export const UPLOAD_API_ROUTE = 'upload'

/**
 * The form-data key for the file-contents when uploading file-contents
 */
export const UPLOAD_FILE_FORM_KEY = 'files'

/**
 * The api-route to be appended to the base-rul when accessing the graphql-api in workspaces
 */
const GRAPHQL_API_ROUTE = 'graphql'

/**
 * The api-route to be appended to the base-url when accessing processes
 */
const PROCESS_API_ROUTE = 'process'

/**
 * Extracts the relative file path from a url to read files from
 *
 * Throws an error if the path is not a valid url to read files from
 *
 * Example:
 * * `https://codefreak.test/files/foo.txt` => `foo.txt`
 * * `https://codefreak.test/files/bar/foo.txt` => `bar/foo.txt`
 * * `https://codefreak.test/foo.txt` => Error
 * * `foo.txt` => Error
 * * `` => Error
 *
 * @param path the url with the file path, see FILES_API_ROUTE for the correct path
 */
export const extractRelativeFilePath = (path: string) => {
  const pattern = `/${FILES_API_ROUTE}/`
  const index = path.indexOf(pattern)

  // index === 0 when no base-url is given, index < 0 when the api pattern is missing
  if (index <= 0) {
    throw new Error(`${path} is not a valid path`)
  }

  return path.substr(index + pattern.length)
}

/**
 * Returns an url for uploading files to a workspace build on the given base-url
 *
 * @param baseUrl the base-url for the workspace
 */
export const uploadFilePath = (baseUrl: string) => {
  return withTrailingSlash(baseUrl) + UPLOAD_API_ROUTE
}

/**
 * Returns an url for reading files from a workspace build on the given base-url
 *
 * @param baseUrl the base-url for the workspace
 * @param filePath the path of the file to read
 */
export const readFilePath = (baseUrl: string, filePath: string) => {
  return (
    withTrailingSlash(baseUrl) + FILES_API_ROUTE + withLeadingSlash(filePath)
  )
}

/**
 * Returns an url for creating an empty file in a workspace built on the given base-url
 *
 * @param baseUrl the base-url for the workspace
 * @param filePath the path of the file to create
 */
export const createFilePath = (baseUrl: string, filePath: string) =>
  trimTrailingSlashes(
    withTrailingSlash(baseUrl) + FILES_API_ROUTE + withLeadingSlash(filePath)
  )

/**
 * Returns an url for creating an empty directory in a workspace built on the given base-url
 *
 * @param baseUrl the base-url for the workspace
 * @param directoryPath the path of the directory to create
 */
export const createDirectoryPath = (baseUrl: string, directoryPath: string) =>
  withTrailingSlash(
    withTrailingSlash(baseUrl) +
      FILES_API_ROUTE +
      withLeadingSlash(directoryPath)
  )

/**
 * Returns an url for deleting a file or directory in a workspace built on the given base-url
 *
 * @param baseUrl the base-url for the workspace
 * @param path the path of the file or directory to delete
 */
export const deletePath = (baseUrl: string, path: string) =>
  trimTrailingSlashes(
    withTrailingSlash(baseUrl) + FILES_API_ROUTE + withLeadingSlash(path)
  )

/**
 * Replaces a `http` or `https` protocol with `ws` or `wss` respectively
 *
 * Throws an error when the given url does not contain `http`
 *
 * @param url the url to convert
 */
export const httpToWs = (url: string) => {
  if (!url.includes('http')) {
    throw new Error(
      'The url could not be converted because it does not contain http'
    )
  }

  return url.replace('http', 'ws')
}

/**
 * Returns an url for accessing the graphql-api in a workspace built on the given base-url
 *
 * @param baseUrl the base-url for the workspace
 */
export const graphqlWebSocketPath = (baseUrl: string) => {
  const url = withTrailingSlash(baseUrl) + GRAPHQL_API_ROUTE
  return httpToWs(url)
}

/**
 * Returns an url for accessing a process in a workspace built on the given base-url
 *
 * @param baseUrl the base-url for the workspace
 * @param processId the id of the process to access
 */
export const processWebSocketPath = (baseUrl: string, processId: string) => {
  if (processId.length === 0) {
    throw new Error('No valid process-id was given')
  }

  const url =
    withTrailingSlash(baseUrl) + PROCESS_API_ROUTE + withLeadingSlash(processId)

  return httpToWs(url)
}

export interface RequestInitWithAuthentication extends RequestInit {
  authToken: string
}

/**
 * Executes a `fetch` with an authorization token provided in the header
 * The token must be provided in `init`
 *
 * @param input see `fetch`
 * @param init see `fetch`, extended with an authorization token
 */
export const fetchWithAuthentication = async (
  input: RequestInfo,
  init: RequestInitWithAuthentication
) => {
  const { authToken, ...otherInit } = init

  const headers =
    authToken !== NO_AUTH_TOKEN
      ? {
          ...otherInit.headers,
          Authorization: `Bearer ${authToken}`
        }
      : otherInit.headers

  const response = await fetch(input, {
    ...otherInit,
    headers
  })

  if (response.status === 401) {
    messageService.error('Your workspace session token is invalid')
    throw new Error('Your workspace session token is invalid')
  }

  return response
}
