import {
  DeleteOutlined,
  EditOutlined,
  FileAddOutlined,
  FolderAddOutlined
} from '@ant-design/icons'
import { Menu } from 'antd'
import { RightClickedItem } from './FileTree'

interface FileTreeRightClickMenuProps {
  rightClickedItem?: RightClickedItem
  onRename?: () => void
  onDelete?: () => void
  onAddFile?: () => void
  onAddDirectory?: () => void
}

const FileTreeRightClickMenu = ({
  rightClickedItem,
  onRename,
  onDelete,
  onAddFile,
  onAddDirectory
}: FileTreeRightClickMenuProps) => {
  const fileBasedMenuItems =
    rightClickedItem !== undefined
      ? [
          <Menu.Item icon={<EditOutlined />} onClick={onRename} disabled>
            Rename
          </Menu.Item>,
          <Menu.Item
            icon={<DeleteOutlined />}
            style={{ color: 'red' }}
            onClick={onDelete}
          >
            Delete
          </Menu.Item>,
          <Menu.Divider />
        ]
      : null

  return (
    <Menu>
      {fileBasedMenuItems}
      <Menu.Item icon={<FileAddOutlined />} onClick={onAddFile}>
        Add file
      </Menu.Item>
      <Menu.Item icon={<FolderAddOutlined />} onClick={onAddDirectory}>
        Add directory
      </Menu.Item>
    </Menu>
  )
}

export default FileTreeRightClickMenu
