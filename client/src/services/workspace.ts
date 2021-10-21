import { NO_AUTH_TOKEN } from '../hooks/workspace/useWorkspace'
import { withLeadingSlash, withTrailingSlash } from './strings'

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
export const fetchWithAuthentication = (
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

  return fetch(input, {
    ...otherInit,
    headers
  })
}
