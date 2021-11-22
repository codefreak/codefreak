import { Button, Col, Input, Row } from 'antd'
import React, { useState } from 'react'
import { GetAnswerDocument, useSaveVersionMutation } from '../generated/graphql'
import { messageService } from '../services/message'

const VersionSaveComponent: React.FC<{
  answerId: string
}> = props => {
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
  )
}
export default VersionSaveComponent
