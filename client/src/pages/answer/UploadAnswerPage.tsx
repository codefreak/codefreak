import { Button, Card, Modal } from 'antd'
import moment from 'moment'
import { useContext, useState } from 'react'
import AnswerBlocker from '../../components/AnswerBlocker'
import ArchiveDownload from '../../components/ArchiveDownload'
import AsyncPlaceholder from '../../components/AsyncContainer'
import FileImport from '../../components/FileImport'
import {
  Answer,
  FileContextType,
  Submission,
  useGetAnswerQuery,
  useImportAnswerSourceMutation,
  useUploadAnswerSourceMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { displayName } from '../../services/user'
import { DifferentUserContext } from '../task/TaskPage'
import FileBrowser from '../../components/FileBrowser'
import { UploadOutlined } from '@ant-design/icons'

export type AnswerWithSubmissionDeadline = Pick<Answer, 'id'> & {
  submission: Pick<Submission, 'deadline'>
}

interface UploadAnswerProps {
  answer: AnswerWithSubmissionDeadline
  onUpload: () => unknown
}

export const UploadAnswerPageButton = ({ answerId }: UploadAnswerPageProps) => {
  const [modalVisible, setModalVisible] = useState(false)
  const showModal = () => setModalVisible(true)
  const hideModal = () => setModalVisible(false)

  return (
    <>
      <Button icon={<UploadOutlined />} type="default" onClick={showModal}>
        Upload Answer
      </Button>
      <Modal
        visible={modalVisible}
        onCancel={hideModal}
        title="Upload Answer"
        footer={[
          <Button type="default" onClick={hideModal} key="cancel">
            Cancel
          </Button>
        ]}
        width="80%"
      >
        <UploadAnswerPage answerId={answerId} />
      </Modal>
    </>
  )
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

export type UploadAnswerPageProps = {
  answerId: string
}

const UploadAnswerPage = ({ answerId }: UploadAnswerPageProps) => {
  const result = useGetAnswerQuery({
    variables: { id: answerId }
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
        <FileBrowser
          key={`answer-rev-${answerRevision}`}
          id={answer.id}
          type={FileContextType.Answer}
        />
      </Card>
      {!differentUser && (
        <>
          <UploadAnswer answer={answer} onUpload={incrementAnswerRevision} />
        </>
      )}
    </>
  )
}

export default UploadAnswerPage
