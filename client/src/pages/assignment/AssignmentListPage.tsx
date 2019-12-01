import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button } from 'antd'
import React from 'react'
import { Link } from 'react-router-dom'
import AsyncContainer from '../../components/AsyncContainer'
import Authorized from '../../components/Authorized'
import useAuthenticatedUser from '../../hooks/useAuthenticatedUser'
import {
  GetAssignmentsProps,
  withGetAssignments
} from '../../services/codefreak-api'

const AssignmentListPage: React.FC<GetAssignmentsProps> = props => {
  const user = useAuthenticatedUser()
  return (
    <AsyncContainer data={props.data}>
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
      <br />
      {JSON.stringify(props.data.assignments)}
    </AsyncContainer>
  )
}

export default withGetAssignments()(AssignmentListPage)
