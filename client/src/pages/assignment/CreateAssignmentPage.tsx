import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Button, Card } from 'antd'
import { useHistory } from 'react-router-dom'
import { useCreateAssignmentMutation } from '../../generated/graphql'
import { Entity, getEntityPath } from '../../services/entity-path'
import { messageService } from '../../services/message'

const CreateAssignmentPage: React.FC = () => {
  const [
    createAssignmentMutation,
    { loading: creatingAssignment }
  ] = useCreateAssignmentMutation()
  const history = useHistory()

  const onAssignmentCreated = (assignment: Entity) => {
    history.push(getEntityPath(assignment))
    messageService.success('Assignment created')
  }

  const createAssignment = async () => {
    const result = await createAssignmentMutation()
    if (result.data) {
      onAssignmentCreated(result.data.createAssignment)
    }
  }

  return (
    <>
      <PageHeaderWrapper />
      <Card title="From Scratch" style={{ marginBottom: 16 }}>
        <div style={{ textAlign: 'center' }}>
          <Button
            onClick={createAssignment}
            size="large"
            type="primary"
            loading={creatingAssignment}
            block
          >
            Create Empty Assignment
          </Button>
        </div>
      </Card>
    </>
  )
}

export default CreateAssignmentPage
