import { Card } from 'antd'
import React from 'react'
import ArchiveDownload from '../../components/ArchiveDownload'
import AsyncPlaceholder from '../../components/AsyncContainer'
import FileImport from '../../components/FileImport'
import { useGetAnswerQuery } from '../../services/codefreak-api'

const AnswerPage: React.FC<{ answerId: string }> = props => {
  const result = useGetAnswerQuery({
    variables: { id: props.answerId }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const { answer } = result.data

  return (
    <>
      <Card
        title="Submit source code"
        extra={<ArchiveDownload url={answer.sourceUrl}>Download source code</ArchiveDownload>}
      >
        <FileImport />
      </Card>
    </>
  )
}

export default AnswerPage
