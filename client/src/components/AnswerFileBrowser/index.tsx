import { FileOutlined } from '@ant-design/icons'
import { Col, Result, Row, Tabs } from 'antd'
import { basename } from 'path'
import React, { useEffect, useState } from 'react'
import {
  FileType,
  useGetAnswerFileListQuery
} from '../../services/codefreak-api'
import AsyncPlaceholder from '../AsyncPlaceholder'
import Centered from '../Centered'
import CodeViewer from '../CodeViewer'
import AnswerFileTree from './AnswerFileTree'

import './index.less'

interface FileBrowserFile {
  path: string
  type: FileType
}

export interface AnswerFileBrowserProps {
  answerId: string
  review?: boolean
  onReady?: (refetch: () => void) => void
}

const AnswerFileBrowser: React.FC<AnswerFileBrowserProps> = props => {
  const { answerId, review, onReady } = props
  const [openedFiles, setOpenedFiles] = useState<FileBrowserFile[]>([])
  const [currentFile, setCurrentFile] = useState<FileBrowserFile>()

  const filesQuery = useGetAnswerFileListQuery({
    variables: { id: answerId }
  })

  useEffect(() => {
    if (onReady && filesQuery.data && filesQuery.refetch) {
      onReady(filesQuery.refetch)
    }
  }, [onReady, filesQuery.data, filesQuery.refetch])

  useEffect(() => {
    const newAnswerFiles = filesQuery.data?.answerFiles
    if (newAnswerFiles) {
      // close files that have been removed by a reload
      const newOpened = openedFiles.filter(file => {
        return newAnswerFiles.some(predicate => predicate.path === file.path)
      })
      if (newOpened.length !== openedFiles.length) {
        setOpenedFiles(newOpened)
        // close the current opened file if it has been removed
        if (currentFile && !newOpened.includes(currentFile)) {
          setCurrentFile(newOpened.length ? newOpened[0] : undefined)
        }
      }
    }
  }, [
    filesQuery.data,
    openedFiles,
    setOpenedFiles,
    currentFile,
    setCurrentFile
  ])

  if (filesQuery.data === undefined) {
    return <AsyncPlaceholder result={filesQuery} />
  }

  const onSelectFileInTree = (file: FileBrowserFile) => {
    // only open files in editor
    if (!file || file.type !== FileType.File) {
      return
    }

    setCurrentFile(file)
    if (file && openedFiles.indexOf(file) === -1) {
      setOpenedFiles([...openedFiles, file])
    }
  }

  const onSelectTab = (path: string) => {
    setCurrentFile(openedFiles.find(file => file.path === path))
  }

  const onTabEdit = (
    path: string | React.MouseEvent<Element, MouseEvent> | React.KeyboardEvent,
    action: 'add' | 'remove'
  ) => {
    if (action === 'remove') {
      const newFiles = openedFiles.filter(file => file.path !== path)
      setOpenedFiles(newFiles)
      // switch to first tab by default after closing the current one
      setCurrentFile(newFiles.length ? newFiles[0] : undefined)
    }
  }

  return (
    <Row className="answer-editor">
      <Col span={6} className="answer-editor-file-tree">
        <h4 className="answer-editor-file-title"> Files </h4>
        <AnswerFileTree
          files={filesQuery.data.answerFiles}
          onFileSelect={onSelectFileInTree}
        />
      </Col>
      <Col span={18} className="answer-editor-code-col">
        <Tabs
          animated={false}
          type="editable-card"
          hideAdd
          className="answer-editor-tabs"
          activeKey={currentFile ? currentFile.path : undefined}
          onTabClick={onSelectTab}
          onEdit={onTabEdit}
        >
          {openedFiles.map(file => (
            <Tabs.TabPane key={file.path} tab={basename(file.path)} />
          ))}
        </Tabs>
        <div className="answer-editor-content">
          {currentFile ? (
            <CodeViewer
              answerId={answerId}
              path={currentFile.path}
              review={review === true}
            />
          ) : (
            <Centered>
              <Result
                title="Please select a file from the tree to view its content"
                icon={<FileOutlined twoToneColor="red" />}
              />
            </Centered>
          )}
        </div>
      </Col>
    </Row>
  )
}

export default AnswerFileBrowser
