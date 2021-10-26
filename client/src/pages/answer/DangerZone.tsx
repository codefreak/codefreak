import { ExclamationCircleTwoTone, SettingOutlined } from '@ant-design/icons'
import { Modal, Button, Row, Col, Alert } from 'antd'
import moment from 'moment'
import { useState } from 'react'
import useMomentReached from '../../hooks/useMomentReached'
import { useServerNow } from '../../hooks/useServerTimeOffset'
import { useResetAnswerMutation } from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { AnswerWithSubmissionDeadline } from './UploadAnswerPage'

interface DangerZoneProps {
  answer: AnswerWithSubmissionDeadline
  onReset: () => void
}

export const DangerZoneButton = ({ answer, onReset }: DangerZoneProps) => {
  const [modalVisible, setModalVisible] = useState(false)
  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)

  return (
    <>
      <Button icon={<SettingOutlined />} type="default" onClick={showModal} />
      <Modal
        visible={modalVisible}
        onCancel={hideModal}
        title="Danger Zone"
        footer={[
          <Button type="default" onClick={hideModal} key="close">
            Close
          </Button>
        ]}
        width="80%"
      >
        <DangerZone answer={answer} onReset={onReset} />
      </Modal>
    </>
  )
}

const DangerZone: React.FC<DangerZoneProps> = ({ answer, onReset }) => {
  const { id } = answer
  const { deadline } = answer.submission
  const [resetAnswer, { loading: resetLoading }] = useResetAnswerMutation({
    variables: { id }
  })
  const serverNow = useServerNow()
  const deadlineReached = useMomentReached(
    deadline ? moment(deadline) : undefined,
    serverNow
  )

  if (deadlineReached === true) {
    return null
  }

  const onResetClick = () => {
    Modal.confirm({
      title: 'Really reset files?',
      icon: <ExclamationCircleTwoTone twoToneColor="#ff4d4f" />,
      okType: 'danger',
      content: (
        <>
          This will REMOVE all modifications you made!
          <br />
          Are you sure?
        </>
      ),
      onOk: () =>
        resetAnswer().then(() => {
          messageService.success('Answer has been reset to initial files!')
          onReset()
        })
    })
  }

  return (
    <Row>
      <Col xl={12}>
        <h3>Reset answer</h3>
        <Alert
          type="error"
          style={{ marginBottom: 16 }}
          message={
            <>
              This will remove all your work on this task and replace everything
              with the initial files from your teacher!
              <br />
              <strong>You cannot revert this action!</strong>
            </>
          }
        />
        <Button danger onClick={onResetClick} loading={resetLoading}>
          Reset all files
        </Button>
      </Col>
    </Row>
  )
}

export default DangerZone
