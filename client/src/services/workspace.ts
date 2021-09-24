const FILES_API_ROUTE = 'files'
const GRAPHQL_API_ROUTE = 'graphql'
const PROCESS_API_ROUTE = 'process'
export const WRITE_FILE_FORM_KEY = 'files'

export const extractRelativeFilePath = (path: string) => {
  const pattern = `/${FILES_API_ROUTE}/`
  const index = path.indexOf(pattern)

  if (index === -1) {
    throw new Error('Invalid file path pattern')
  }

  return path.substr(index + pattern.length)
}

export const writeFilePath = (baseUrl: string) => {
  const separator = baseUrl.endsWith('/') ? '' : '/'
  return `${baseUrl}${separator}${FILES_API_ROUTE}`
}

export const normalizePath = (path: string) => {
  const trimmedPath = path.trim()
  return trimmedPath.endsWith('/') ? trimmedPath : `${trimmedPath}/`
}

export const readFilePath = (baseUrl: string, filePath: string) => {
  const filePathSeparator = filePath.startsWith('/') ? '' : '/'
  const normalizedBaseUrl = normalizePath(baseUrl)
  return `${normalizedBaseUrl}${FILES_API_ROUTE}${filePathSeparator}${filePath}`
}

export const httpToWs = (url: string) => url.replace('http', 'ws')

export const graphqlWebSocketPath = (baseUrl: string) => {
  const normalizedBaseUrl = normalizePath(baseUrl)
  return httpToWs(`${normalizedBaseUrl}${GRAPHQL_API_ROUTE}`)
}

export const processWebSocketPath = (baseUrl: string, processId: string) => {
  const normalizedBaseUrl = normalizePath(baseUrl)
  return httpToWs(`${normalizedBaseUrl}${PROCESS_API_ROUTE}/${processId}`)
}
