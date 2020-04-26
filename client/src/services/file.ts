/**
 * Check value for non-printable characters
 *
 * @param value
 */
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
