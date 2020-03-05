import { File, FileType } from '../generated/graphql'

export interface FileTreeNode extends Pick<File, 'path' | 'type'> {}

export interface FileTreeDirectoryNode extends FileTreeNode {
  children?: Array<FileTreeNode | FileTreeDirectoryNode>
}

/**
 * Get the full directory path that contains the file/directory
 * Follows official UNIX rules for dirname
 *
 * Examples:
 * /a/b -> /a
 * ./a/b -> ./a
 * /a -> /
 * . -> .
 * / -> /
 * a -> a
 *
 * @param path
 */
export const dirname = (path: string): string => {
  // match everything before last slash
  // remaining path will contain a trailing slash!
  const matches = path.match(/^(\/?.*?)[^/]*\/?$/)
  // in case the path does not contain any slashes return the original path
  if (!matches || !matches[1]) {
    return path
  }
  // if the remaining path is only a single letter, return it
  if (matches[1].length === 1) {
    return matches[1]
  }
  // in all other cases return the remaining path without trailing slash
  return normalizePath(matches[1])
}

/**
 * Get filename from path including extension
 *
 * @param path
 */
export const basename = (path: string) => {
  return (
    normalizePath(path)
      .split('/')
      .pop() || '.'
  )
}

/**
 * Remove trailing slashes from path
 *
 * @param path
 */
export const normalizePath = (path: string) => {
  return path.replace(/\/+$/g, '')
}

/**
 * Make "relative" virtual paths absolute
 * so foo becomes /foo or ./bar/baz becomes /bar/baz
 *
 * @param path
 */
export const abspath = (path: string) => {
  return '/' + normalizePath(path).replace(/^\.?\/*/g, '')
}

export const fileListToTree = (
  list: FileTreeNode[]
): FileTreeNode | FileTreeDirectoryNode | undefined => {
  const listCopy = list.slice()
  listCopy.sort((a, b) => abspath(a.path).localeCompare(abspath(b.path)))
  // get the alphabetically sorted first path name as root
  const rootNode = listCopy.shift()

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
