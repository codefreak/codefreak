import {
  DownCircleOutlined,
  ExclamationCircleTwoTone,
  UpCircleOutlined
} from '@ant-design/icons'
import { Alert, Button, Card, Col, Modal, Row } from 'antd'
import moment from 'moment'
import { useCallback, useContext, useState } from 'react'
import AnswerBlocker from '../../components/AnswerBlocker'
import ArchiveDownload from '../../components/ArchiveDownload'
import AsyncPlaceholder from '../../components/AsyncContainer'
import FileImport from '../../components/FileImport'
import useMomentReached from '../../hooks/useMomentReached'
import { useServerNow } from '../../hooks/useServerTimeOffset'
import {
  Answer,
  FileContextType,
  Submission,
  useGetAnswerQuery,
  useImportAnswerSourceMutation,
  useResetAnswerMutation,
  useUploadAnswerSourceMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { displayName } from '../../services/user'
import { DifferentUserContext } from '../task/TaskPage'
import FileBrowser from '../../components/FileBrowser'

type AnswerWithSubmissionDeadline = Pick<Answer, 'id'> & {
  submission: Pick<Submission, 'deadline'>
}

interface DangerZoneProps {
  answer: AnswerWithSubmissionDeadline
  onReset: () => void
}

const DangerZone: React.FC<DangerZoneProps> = props => {
  const { id } = props.answer
  const { deadline } = props.answer.submission
  const [resetAnswer, { loading: resetLoading }] = useResetAnswerMutation({
    variables: { id }
  })
  const [showDangerZone, setShowDangerZone] = useState<boolean>(false)
  const toggleDangerZone = useCallback(() => {
    setShowDangerZone(!showDangerZone)
  }, [showDangerZone, setShowDangerZone])
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
          props.onReset()
        })
    })
  }

  const upCircle = <UpCircleOutlined />
  const downCircle = <DownCircleOutlined />

  return (
    <Card
      title="Danger Zone"
      extra={
        <Button
          icon={showDangerZone ? upCircle : downCircle}
          onClick={toggleDangerZone}
        >
          {showDangerZone ? 'Hide' : 'Show'}
        </Button>
      }
      bodyStyle={{ display: `${showDangerZone ? '' : 'none'}` }}
    >
      <Row>
        <Col xl={12}>
          <h3>Reset answer</h3>
          <Alert
            type="error"
            style={{ marginBottom: 16 }}
            message={
              <>
                This will remove all your work on this task and replace
                everything with the initial files from your teacher!
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
    </Card>
  )
}

interface UploadAnswerProps {
  answer: AnswerWithSubmissionDeadline
  onUpload: () => unknown
}

const UploadAnswer: React.FC<UploadAnswerProps> = props => {
  const { id } = props.answer
  const { deadline } = props.answer.submission
  const [uploadSource, { loading: uploading }] = useUploadAnswerSourceMutation()
  const [importSource, { loading: importing }] = useImportAnswerSourceMutation()

  const onUpload = (files: File[]) =>
    uploadSource({ variables: { files, id } }).then(() => {
      messageService.success('Source code uploaded successfully')
      props.onUpload()
    })
  const onImport = (url: string) =>
    importSource({ variables: { url, id } }).then(() => {
      messageService.success('Source code imported successfully')
      props.onUpload()
    })

  return (
    <Card title="Upload Source Code" style={{ marginBottom: '16px' }}>
      <AnswerBlocker deadline={deadline ? moment(deadline) : undefined}>
        <FileImport
          uploading={uploading}
          onUpload={onUpload}
          onImport={onImport}
          importing={importing}
        />
      </AnswerBlocker>
    </Card>
  )
}

const AnswerPage: React.FC<{ answerId: string }> = props => {
  const result = useGetAnswerQuery({
    variables: { id: props.answerId }
  })
  const differentUser = useContext(DifferentUserContext)

  // This fake "revision" is a dirty hack to force a full reload of the file-browser
  // after files have been modified externally via upload/import or reset button.
  // We cannot invalidate Apollo caches for all paths of the file-browser easily
  // (see useFileCollection Hook) and we do not have a real collection revision
  // on the backend, yet.
  const [answerRevision, setAnswerRevision] = useState(0)
  const incrementAnswerRevision = () =>
    setAnswerRevision(revision => revision + 1)

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { answer } = result.data

  const filesTitle = differentUser
    ? `Files uploaded by ${displayName(differentUser)}`
    : 'Your current submission'

  return (
    <>
      <Card
        title={filesTitle}
        style={{ marginBottom: '16px' }}
        extra={
          <ArchiveDownload url={answer.sourceUrl}>
            Download source code
          </ArchiveDownload>
        }
      >
        {differentUser && (
          <Alert
            showIcon
            message="You can add comments inside code by clicking the + symbol next to the line numbers!"
            style={{ marginBottom: 16 }}
          />
        )}
        <FileBrowser
          key={`answer-rev-${answerRevision}`}
          id={answer.id}
          type={FileContextType.Answer}
        />
      </Card>
      {!differentUser && (
        <>
          <UploadAnswer answer={answer} onUpload={incrementAnswerRevision} />
          <DangerZone answer={answer} onReset={incrementAnswerRevision} />
        </>
      )}
    </>
  )
}

export default AnswerPage
