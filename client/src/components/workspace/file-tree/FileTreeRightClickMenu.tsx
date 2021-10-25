import {
  DeleteOutlined,
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
  onDelete,
  onAddFile,
  onAddDirectory
}: FileTreeRightClickMenuProps) => {
  const fileBasedMenuItems =
    rightClickedItem !== undefined
      ? [
          // TODO enable once the api-endpoint is ready
          // <Menu.Item icon={<EditOutlined />} onClick={onRename}>
          //   Rename
          // </Menu.Item>,
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
