import { Card } from 'antd'
import React, { useContext, useEffect } from 'react'
import AnswerFileBrowser from '../../components/AnswerFileBrowser'
import ArchiveDownload from '../../components/ArchiveDownload'
import AsyncPlaceholder from '../../components/AsyncContainer'
import FileImport from '../../components/FileImport'
import {
  Answer,
  useGetAnswerQuery,
  useImportAnswerSourceMutation,
  useUploadAnswerSourceMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { displayName } from '../../services/user'
import { DifferentUserContext } from '../task/TaskPage'

const UploadAnswer: React.FC<{ answer: Pick<Answer, 'id'> }> = ({
  answer: { id }
}) => {
  const [
    uploadSource,
    { loading: uploading, data: uploadSuccess }
  ] = useUploadAnswerSourceMutation()

  const [
    importSource,
    { loading: importing, data: importSucess }
  ] = useImportAnswerSourceMutation()

  const onUpload = (files: File[]) => uploadSource({ variables: { files, id } })

  const onImport = (url: string) => importSource({ variables: { url, id } })

  useEffect(() => {
    if (uploadSuccess || importSucess) {
      messageService.success('Source code submitted successfully')
    }
  }, [uploadSuccess, importSucess])

  return (
    <Card title="Upload Source Code">
      <FileImport
        uploading={uploading}
        onUpload={onUpload}
        onImport={onImport}
        importing={importing}
      />
    </Card>
  )
}

const AnswerPage: React.FC<{ answerId: string }> = props => {
  const result = useGetAnswerQuery({
    variables: { id: props.answerId }
  })
  const differentUser = useContext(DifferentUserContext)

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
        <AnswerFileBrowser answerId={answer.id} />
      </Card>
      <UploadAnswer answer={answer} />
    </>
  )
}

export default AnswerPage
