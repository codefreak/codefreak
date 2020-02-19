import { File, FileType } from '../generated/graphql'

export interface FileSystemNode extends Pick<File, 'path' | 'type'> {}

export interface FileSystemDirectoryNode extends FileSystemNode {
  children?: FileSystemDirectoryNode[]
}

export const dirname = (path: string) => {
  const normalizedPath = normalizePath(path)
  const parts = normalizedPath.split('/')
  parts.pop()
  if (parts.length > 1) {
    return parts.join('/')
  } else if (parts.length === 1) {
    return parts.pop()
  }
  return normalizedPath || path
}

export const basename = (path: string) => {
  return (
    normalizePath(path)
      .split('/')
      .pop() || '.'
  )
}

export const normalizePath = (path: string) => {
  // remove trailing slashes
  return path.replace(/\/+$/g, '')
}

export const fileListToTree = (
  list: FileSystemNode[]
): FileSystemDirectoryNode | undefined => {
  const listCopy = list.slice()
  // get the alphabetically sorted first path name as root
  listCopy.sort((a, b) => a.path.localeCompare(b.path))
  const rootNode = listCopy.shift()

  const filterChildren = (
    candidates: FileSystemNode[],
    parent: FileSystemDirectoryNode
  ) => {
    const parentPath = normalizePath(parent.path)
    return candidates.filter(file => dirname(file.path) === parentPath)
  }

  const mergeChildren = (
    candidates: FileSystemNode[],
    parent: FileSystemDirectoryNode
  ): FileSystemDirectoryNode => {
    if (parent.type !== FileType.Directory) {
      return { ...parent }
    }

    const children = filterChildren(candidates, parent)
    // do not pass already assigned children further down
    const remainingChildren = candidates.filter(
      file => children.indexOf(file) === -1
    )
    return {
      ...parent,
      children: children.map(child => {
        return mergeChildren(remainingChildren, child)
      })
    }
  }

  return rootNode ? mergeChildren(listCopy, rootNode) : undefined
}
