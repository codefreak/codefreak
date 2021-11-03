import React from 'react'
import { List, Tooltip } from 'antd'
import { EditOutlined, InfoCircleFilled } from '@ant-design/icons'
import { EditStringArrayButton } from './EditStringArrayModal'

export interface EditableStringListProps {
  dataSource: string[]
  onChangeValue: (newList: string[]) => unknown
  title: string
  tooltipHelp: string
  editHelp: React.ReactNode
}

const renderStringListItem = (value: string) => (
  <List.Item>
    <code>{value}</code>
  </List.Item>
)

const EditableStringList: React.FC<EditableStringListProps> = props => {
  const { dataSource, onChangeValue, tooltipHelp, editHelp, title } = props
  return (
    <List
      size="small"
      header={
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}
        >
          <span>
            <b>{title}</b>{' '}
            <Tooltip title={tooltipHelp} placement="bottom">
              <InfoCircleFilled />
            </Tooltip>
          </span>
          <EditStringArrayButton
            title={title}
            extraContent={editHelp}
            initialValues={dataSource}
            onSave={onChangeValue}
            icon={<EditOutlined />}
            type="link"
          />
        </div>
      }
      bordered
      dataSource={dataSource}
      renderItem={renderStringListItem}
    />
  )
}

export default EditableStringList
