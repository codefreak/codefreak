const FILES_API_ROUTE = 'files'
export const WRITE_FILE_FORM_KEY = 'files'

export const extractRelativeFilePath = (path: string) => {
  const pattern = `/${FILES_API_ROUTE}/`
  const index = path.indexOf(pattern)

  // index === 0 when no base-url is given, index < 0 when the api pattern is missing
  if (index <= 0) {
    throw new Error(`${path} is not a valid path`)
  }

  return path.substr(index + pattern.length)
}

export const writeFilePath = (baseUrl: string) => {
  const separator = baseUrl.endsWith('/') ? '' : '/'
  return `${baseUrl}${separator}${FILES_API_ROUTE}`
}

export const withTrailingSlash = (path: string) => {
  const trimmedPath = path.trim()
  return trimmedPath.endsWith('/') ? trimmedPath : `${trimmedPath}/`
}

export const readFilePath = (baseUrl: string, filePath: string) => {
  const filePathSeparator = filePath.startsWith('/') ? '' : '/'
  const normalizedBaseUrl = withTrailingSlash(baseUrl)
  return `${normalizedBaseUrl}${FILES_API_ROUTE}${filePathSeparator}${filePath}`
}
