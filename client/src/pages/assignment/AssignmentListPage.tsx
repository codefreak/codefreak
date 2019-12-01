import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button } from 'antd'
import React from 'react'
import { Link } from 'react-router-dom'
import Authorized from '../../components/Authorized'
import useAuthenticatedUser from '../../hooks/useAuthenticatedUser'

const AssignmentListPage: React.FC = () => {
  const user = useAuthenticatedUser()

  return (
    <>
      <PageHeaderWrapper
        extra={
          <Authorized role="TEACHER">
            <Link to="/assignments/create" key="1">
              <Button type="primary" icon="plus">
                Create
              </Button>
            </Link>
          </Authorized>
        }
      />
      Hello {user.roles}
      <br />
      <Link to="/assignments/1337">Sample Assignment</Link>
    </>
  )
}

export default AssignmentListPage
