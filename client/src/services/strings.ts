/**
 * Create an ellipsis if the given string is longer than {max} chars.
 * Returns the input string otherwise.
 *
 * @param str The string to be limited
 * @param max Maximum number of chars
 * @param ellipsisChar Optional a custom ellipsis symbol (default is …/U+2026)
 */
export const ellipsis = (
  str: string,
  max: number,
  ellipsisChar: string = '…'
) => {
  if (str.length <= max) {
    return str
  }
  return str.substr(0, max) + ellipsisChar
}
