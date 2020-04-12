import { dirname, resolve } from 'path'
import { File, FileType } from '../generated/graphql'

export interface FileTreeNode extends Pick<File, 'path' | 'type'> {}

export interface FileTreeDirectoryNode extends FileTreeNode {
  children?: Array<FileTreeNode | FileTreeDirectoryNode>
}

/**
 * Make "relative" virtual paths absolute
 * so foo becomes /foo or ./bar/baz becomes /bar/baz
 *
 * @param path
 */
export const abspath = (path: string) => {
  return resolve('/', path)
}

export const fileListToTree = (
  list: FileTreeNode[]
): FileTreeNode | FileTreeDirectoryNode | undefined => {
  const listCopy = list.slice()
  listCopy.sort((a, b) => abspath(a.path).localeCompare(abspath(b.path)))
  // get the alphabetically sorted first path name as root
  let rootNode = listCopy.shift()

  // there are archives that omit the root directory, so we create one on demand
  // this is not bullet proof and should be handled on the backend in the future
  if (rootNode && abspath(rootNode.path) !== '/') {
    listCopy.unshift(rootNode)
    rootNode = {
      path: '/',
      type: FileType.Directory
    }
  }

  // create a map with dirname -> file
  const dirnameMap: Record<string, FileTreeNode[]> = {}
  listCopy.forEach(file => {
    const dir = dirname(abspath(file.path))
    if (dirnameMap[dir] === undefined) {
      dirnameMap[dir] = []
    }
    dirnameMap[dir].push(file)
  })

  // adds children to directory nodes recursively
  // adds a copy of each node so we don't modify original objects
  const createTree = (
    parentNode: FileTreeNode
  ): FileTreeNode | FileTreeDirectoryNode => {
    if (parentNode.type !== FileType.Directory) {
      return { ...parentNode }
    }
    return {
      ...parentNode,
      children: (dirnameMap[abspath(parentNode.path)] || []).map(createTree)
    }
  }

  return rootNode ? createTree(rootNode) : undefined
}

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
