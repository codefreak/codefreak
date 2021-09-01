const FILES_API_ROUTE = 'files'
export const WRITE_FILE_FORM_KEY = 'files'

export const extractRelativeFilePath = (path: string) =>
  path.substr(path.indexOf('/files/') + '/files/'.length)

export const writeFilePath = (baseUrl: string) => {
  const separator = baseUrl.endsWith('/') ? '' : '/'
  return `${baseUrl}${separator}${FILES_API_ROUTE}`
}

export const readFilePath = (baseUrl: string, filePath: string) => {
  const baseUrlSeparator = baseUrl.endsWith('/') ? '' : '/'
  const filePathSeparator = filePath.startsWith('/') ? '' : '/'
  return `${baseUrl}${baseUrlSeparator}${FILES_API_ROUTE}${filePathSeparator}${filePath}`
}
