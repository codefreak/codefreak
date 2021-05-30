import { dirname, resolve } from 'path'

export const isBinaryContent = (value: string) => {
  // eslint-disable-next-line no-control-regex
  return /[\x00-\x08\x0E-\x1F]/.test(value)
}

const LINE_REGEX = /\r?\n/g

/**
 * Count the number of lines in a string
 *
 * @param input
 */
export const numberOfLines = (input: string) => {
  return (input.match(LINE_REGEX) || []).length + 1
}

/**
 * Get a slice of a string between two lines INCLUDING start and end lines
 *
 * @param input The input string
 * @param start The first line (1-based)
 * @param end The last line (1-based)
 */
export const sliceLines = (input: string, start?: number, end?: number) => {
  const split = input.split(LINE_REGEX)
  const startIndex = start ? Math.max(start - 1, 0) : undefined
  return split.slice(startIndex, end).join('\n')
}

/**
 * Matches the given files with the accepted file extensions and returns the list of files with invalid extensions.
 *
 * @param fileNames The file names
 * @param acceptedExtensions The accepted file extensions, e.g. [ '.zip' ]
 */
export const findFilesWithInvalidExtension = (
  fileNames: string[],
  acceptedExtensions: string[]
) => {
  const invalidFiles: string[] = []

  fileNames.forEach(fileName => {
    if (!validateFileExtension(fileName, acceptedExtensions)) {
      invalidFiles.push(fileName)
    }
  })

  return invalidFiles
}

/**
 * Matches a file name against a list of accepted file extensions and returns whether the file has an accepted extension.
 *
 * @param fileName The file name
 * @param acceptedExtensions The accepted file extensions, e.g. [ '.zip' ]
 */
const validateFileExtension = (
  fileName: string,
  acceptedExtensions: string[]
) => {
  const fileExtension = extractExtension(fileName)
  let isFileExtensionValid = false

  acceptedExtensions.forEach(extension => {
    if (extension === fileExtension) {
      isFileExtensionValid = true
    }
  })

  return isFileExtensionValid
}

/**
 * Extracts the extension from a file name.
 * Returns an empty string if the file has no extension.
 *
 * @param fileName The file name
 */
const extractExtension = (fileName: string) => {
  const extensionIndex = fileName.lastIndexOf('.')
  // The index has to be greater than 0 because files can start with a '.' but have no extension
  return extensionIndex > 0 ? fileName.substring(extensionIndex) : ''
}

export const supportedArchiveExtensions = [
  '.zip',
  '.tar',
  '.gz',
  '.xz',
  '.Z',
  '.bz2',
  '.tbz2',
  '.tgz',
  '.txz',
  '.jar'
]
const abspath = (path: string) => resolve('/', path)
const isSamePath = (a: string, b: string) => abspath(a) === abspath(b)

/**
 * List all directory names of path and all parents
 *
 * @param path
 */
export const dirnames = (path: string): string[] => {
  const dirs: string[] = []
  while (!isSamePath(dirname(path), path)) {
    dirs.push((path = dirname(path)))
  }
  return dirs
}
