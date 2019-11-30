import { PageHeaderWrapper } from '@ant-design/pro-layout'
import React from 'react'
import { useParams } from 'react-router'

const AssignmentPage: React.FC = () => {
  const { id } = useParams()

  return (
    <>
      <PageHeaderWrapper />
      Assignment Id: {id}
    </>
  )
}

export default AssignmentPage
