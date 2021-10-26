import {
  DeleteOutlined,
  FileAddOutlined,
  FolderAddOutlined
} from '@ant-design/icons'
import { Menu, MenuProps } from 'antd'
import { RightClickedItem } from './FileTree'

/**
 * TODO document
 */
interface FileTreeRightClickMenuProps extends MenuProps {
  /**
   * TODO document
   */
  rightClickedItem?: RightClickedItem

  /**
   * TODO document
   */
  onRename?: () => void

  /**
   * TODO document
   */
  onDelete?: () => void

  /**
   * TODO document
   */
  onAddFile?: () => void

  /**
   * TODO document
   */
  onAddDirectory?: () => void
}

/**
 * TODO document
 */
const FileTreeRightClickMenu = ({
  rightClickedItem,
  onDelete,
  onAddFile,
  onAddDirectory,
  ...menuProps
}: FileTreeRightClickMenuProps) => {
  const fileBasedMenuItems =
    rightClickedItem !== undefined
      ? [
          // TODO enable once the api-endpoint is ready
          // <Menu.Item key="rename" icon={<EditOutlined />} onClick={onRename}>
          //   Rename
          // </Menu.Item>,
          <Menu.Item
            key="delete"
            icon={<DeleteOutlined />}
            style={{ color: 'red' }}
            onClick={onDelete}
          >
            Delete
          </Menu.Item>,
          <Menu.Divider key="divider" />
        ]
      : null

  return (
    <Menu {...menuProps}>
      {fileBasedMenuItems}
      <Menu.Item key="add-file" icon={<FileAddOutlined />} onClick={onAddFile}>
        Add file
      </Menu.Item>
      <Menu.Item
        key="add-directory"
        icon={<FolderAddOutlined />}
        onClick={onAddDirectory}
      >
        Add directory
      </Menu.Item>
    </Menu>
  )
}

export default FileTreeRightClickMenu
