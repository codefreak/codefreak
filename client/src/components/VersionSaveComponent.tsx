import { Button, Col, Dropdown, Input, Menu, Row } from 'antd'
import React, { useState } from 'react'
import {
  FileContextType,
  GetAnswerDocument,
  ListFilesDocument,
  useChangeVersionMutation,
  useGetAnswerQuery,
  useSaveVersionMutation
} from '../generated/graphql'
import { messageService } from '../services/message'
import useEvaluationStatus from '../hooks/useEvaluationStatus'
import { isEvaluationInProgress } from '../services/evaluation'

const VersionSaveComponent: React.FC<{
  answerId: string
}> = props => {
  const result = useGetAnswerQuery({
    variables: { id: props.answerId }
  })

  const status = useEvaluationStatus(props.answerId)

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
            id: props.answerId,
            type: FileContextType.Answer
          },
          path: '/'
        }
      },
      {
        query: GetAnswerDocument,
        variables: {
          id: props.answerId
        }
      }
    ]
  })

  const menuItemList = result.data?.answer.versions.map(commit => (
    <Menu.Item
      key={commit.versionKey}
      onClick={() =>
        changeVersion({
          variables: { id: props.answerId, versionID: commit.versionKey }
        })
      }
    >
      {commit.commitMessage}
    </Menu.Item>
  )) ?? <Menu.Item>loading...</Menu.Item>

  const menu = <Menu>{menuItemList}</Menu>

  const dropdown = (
    <Dropdown overlay={menu} disabled={isEvaluationInProgress(status)}>
      <Button>
        {result.data?.answer.currentVersionName ?? 'Current version'}
      </Button>
    </Dropdown>
  )

  const [inputState, setInputState] = useState('')

  const [saveVersion] = useSaveVersionMutation({
    onCompleted: ({ saveVersion: didSaveVersion }) => {
      if (didSaveVersion) {
        messageService.success('New version saved')
      } else {
        messageService.error('No changes detected, not saving')
      }
    },
    variables: {
      id: props.answerId,
      commitMessage: inputState
    },
    refetchQueries: [
      {
        query: GetAnswerDocument,
        variables: {
          id: props.answerId
        }
      }
    ]
  })

  return (
    <Row justify="end" align="middle" gutter={24}>
      <Col>Submission version:</Col>
      <Col>{dropdown}</Col>
      <Col>
        <Row gutter={8}>
          <Col>
            <Input
              placeholder="Commit Message"
              value={inputState}
              onChange={e => {
                setInputState(e.target.value)
              }}
              onPressEnter={() => {
                saveVersion({
                  variables: { id: props.answerId, commitMessage: inputState }
                })
              }}
            />
          </Col>
          <Col>
            <Button
              disabled={isEvaluationInProgress(status)}
              type="primary"
              onClick={() => {
                saveVersion({
                  variables: { id: props.answerId, commitMessage: inputState }
                })
                setInputState('')
              }}
            >
              Save version
            </Button>
          </Col>
        </Row>
      </Col>
    </Row>
  )
}
export default VersionSaveComponent
