import {
  DeleteOutlined,
  EditOutlined,
  FileAddOutlined,
  FolderAddOutlined
} from '@ant-design/icons'
import { Menu, MenuProps } from 'antd'
import { RightClickedItem } from './FileTree'

/**
 * Extends the default MenuProps with the right-clicked-item and callbacks for the menu-items.
 */
interface FileTreeRightClickMenuProps extends MenuProps {
  /**
   * The tree-item that was right-clicked. Undefined is interpreted as the file-tree root.
   */
  rightClickedItem?: RightClickedItem

  /**
   * Called when the rename item is clicked
   */
  onRename?: () => void

  /**
   * Called when the delete item is clicked
   */
  onDelete?: () => void

  /**
   * Called when the add-file item is clicked
   */
  onAddFile?: () => void

  /**
   * Called when the add-directory item is clicked
   */
  onAddDirectory?: () => void
}

/**
 * Opens a context-menu for when an item in a file-tree was right-clicked.
 * It consists of four actions: add-file and add-directory and when the right-clicked-item
 * was a file or directory also rename and delete.
 */
const FileTreeRightClickMenu = ({
  rightClickedItem,
  onRename,
  onDelete,
  onAddFile,
  onAddDirectory,
  ...menuProps
}: FileTreeRightClickMenuProps) => {
  const fileBasedMenuItems =
    rightClickedItem !== undefined
      ? [
          // TODO enable once the api-endpoint is ready
          <Menu.Item key="rename" icon={<EditOutlined />} onClick={onRename}>
            Rename
          </Menu.Item>,
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
