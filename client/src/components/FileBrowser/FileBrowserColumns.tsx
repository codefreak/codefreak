import { buildDateSorter } from '../../services/time'
import { ColumnsType } from 'antd/es/table'
import { FileBrowserFile } from './interfaces'
import { useFormatter } from '../../hooks/useFormatter'
import React from 'react'

interface SizeColumnProps {
  node: FileBrowserFile
  size: number
}

const SizeColumn: React.FC<SizeColumnProps> = props => {
  const { node, size } = props
  const { bytes } = useFormatter()
  if (node.type === 'directory') {
    return <span>-</span>
  }
  return <span style={{ whiteSpace: 'nowrap' }}>{bytes(size)}</span>
}

interface LastModifiedColumnProps {
  value: string
}

const LastModifiedColumn: React.FC<LastModifiedColumnProps> = props => {
  const { value } = props
  const { dateTime } = useFormatter()
  return <span style={{ whiteSpace: 'nowrap' }}>{dateTime(value)}</span>
}

export default [
  {
    width: '10%',
    key: 'size',
    sorter: (a, b) => (a.size || 0) - (b.size || 0),
    title: 'Size',
    dataIndex: 'size',
    render: (size: number, node) => <SizeColumn node={node} size={size} />
  },
  {
    width: '10%',
    key: 'lastModified',
    title: 'Last Modified',
    dataIndex: 'lastModified',
    sorter: buildDateSorter('lastModified'),
    render: (value: string) => <LastModifiedColumn value={value} />
  }
] as ColumnsType<FileBrowserFile>
