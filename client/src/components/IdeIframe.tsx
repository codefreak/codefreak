import React, { useState } from 'react'
import Centered from './Centered'
import LaunchIdeSteps from './LaunchIdeSteps'
import {
  FileContextType,
  IdeType,
  ListFilesDocument,
  useChangeVersionMutation,
  useGetAnswerQuery
} from '../generated/graphql'
import { Button, Col, Dropdown, Menu, Row } from 'antd'
import VersionSaveComponent from './VersionSaveComponent'
import { messageService } from '../services/message'

const IdeIframe: React.FC<{ type: IdeType; id: string }> = ({ type, id }) => {
  const [ideUrl, setIdeUrl] = useState<string | undefined>()

  const result = useGetAnswerQuery({
    variables: { id }
  })

  const [changeVersion] = useChangeVersionMutation({
    onCompleted: ({ changeVersion: didVersionChange }) => {
      if (didVersionChange) {
        messageService.success('Version got changed')
      } else {
        messageService.error('Selected version is the current version')
      }
      result.refetch()
    },
    refetchQueries: [
      {
        query: ListFilesDocument,
        variables: {
          context: {
            id,
            type: FileContextType.Answer
          },
          path: '/'
        }
      }
    ]
  })

  const menuItemList = result.data?.answer.versions.map(commit => (
    <Menu.Item
      key={commit.versionKey}
      onClick={() =>
        changeVersion({
          variables: { id, versionID: commit.versionKey }
        }).then(() => result.refetch())
      }
    >
      {commit.commitMessage}
    </Menu.Item>
  ))

  const menu = <Menu>{menuItemList}</Menu>

  const dropdown = (
    <Dropdown overlay={menu}>
      <Button>{result.data?.answer.currentVersionName}</Button>
    </Dropdown>
  )

  const versioning = (
    <Row justify="end" align="middle" gutter={24}>
      <Col>Submission version:</Col>
      <Col style={{ minWidth: '20px' }}>{dropdown}</Col>
      <Col>
        <VersionSaveComponent answerId={id} />
      </Col>
    </Row>
  )

  return ideUrl ? (
    <Row gutter={[24, 28]}>
      <Col span={24}>{versioning}</Col>
      <Col span={24}>
        <iframe
          key={`ide-iframe-${id}`}
          className="ide-iframe"
          src={ideUrl}
          title="Edit answer"
        />
      </Col>
    </Row>
  ) : (
    <Centered>
      <LaunchIdeSteps type={type} id={id} onReady={setIdeUrl} />
    </Centered>
  )
}

export default IdeIframe
