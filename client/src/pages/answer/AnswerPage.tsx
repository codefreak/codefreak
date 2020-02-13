import { Card } from 'antd'
import React, { useContext, useEffect } from 'react'
import ArchiveDownload from '../../components/ArchiveDownload'
import AsyncPlaceholder from '../../components/AsyncContainer'
import FileImport from '../../components/FileImport'
import {
  useGetAnswerQuery,
  useImportAnswerSourceMutation,
  useUploadAnswerSourceMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { displayName } from '../../services/user'
import { DifferentUserContext } from '../task/TaskPage'

const AnswerPage: React.FC<{ answerId: string }> = props => {
  const result = useGetAnswerQuery({
    variables: { id: props.answerId }
  })
  const differentUser = useContext(DifferentUserContext)

  const [
    uploadSource,
    { loading: uploading, data: uploadSuccess }
  ] = useUploadAnswerSourceMutation()

  const [
    importSource,
    { loading: importing, data: importSucess }
  ] = useImportAnswerSourceMutation()

  useEffect(() => {
    if (uploadSuccess || importSucess) {
      messageService.success('Source code submitted successfully')
    }
  }, [uploadSuccess, importSucess])

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { answer } = result.data

  const onUpload = (files: File[]) =>
    uploadSource({ variables: { files, id: answer.id } })

  const onImport = (url: string) =>
    importSource({ variables: { url, id: answer.id } })

  if (differentUser) {
    return (
      <Card title={`Files uploaded by ${displayName(differentUser)}`}>
        TODO
      </Card>
    )
  }

  return (
    <>
      <Card
        title="Submit source code"
        extra={
          <ArchiveDownload url={answer.sourceUrl}>
            Download source code
          </ArchiveDownload>
        }
      >
        <FileImport
          uploading={uploading}
          onUpload={onUpload}
          onImport={onImport}
          importing={importing}
        />
      </Card>
    </>
  )
}

export default AnswerPage
