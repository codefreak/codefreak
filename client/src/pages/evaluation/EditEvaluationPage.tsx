import {
  DeleteOutlined,
  PlusOutlined,
  SettingOutlined
} from '@ant-design/icons'
import { Alert, Button, Checkbox, Modal, Tooltip } from 'antd'
import { CardProps } from 'antd/lib/card'
import { useState } from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import CardList from '../../components/CardList'
import {
  GetEvaluationStepDefinitionsQueryResult,
  useCreateEvaluationStepDefinitionMutation,
  useDeleteEvaluationStepDefinitionMutation,
  useGetEvaluationStepDefinitionsQuery,
  useSetEvaluationStepDefinitionPositonMutation,
  useUpdateEvaluationStepDefinitionMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { CheckboxChangeEvent } from 'antd/es/checkbox'
import EditEvaluationStepDefinitionModal from '../../components/EditEvaluationStepDefinitionModal'
import SyntaxHighlighter from '../../components/code/SyntaxHighlighter'

type EvaluationStepDefinition = NonNullable<
  GetEvaluationStepDefinitionsQueryResult['data']
>['task']['evaluationStepDefinitions'][0]

const EditEvaluationPage: React.FC<{ taskId: string }> = ({ taskId }) => {
  const result = useGetEvaluationStepDefinitionsQuery({ variables: { taskId } })
  const [deleteStep] = useDeleteEvaluationStepDefinitionMutation({
    onCompleted: () => {
      messageService.success('Evaluation step deleted')
      result.refetch()
    }
  })
  const [setEvaluationStepDefinitionPosition] =
    useSetEvaluationStepDefinitionPositonMutation()

  const [updateMutation] = useUpdateEvaluationStepDefinitionMutation({
    onCompleted: () => {
      result.refetch()
      messageService.success('Step updated')
    }
  })

  const [createStep] = useCreateEvaluationStepDefinitionMutation()
  const [sureToEdit, setSureToEdit] = useState(false)
  const onSureToEditChange = (e: CheckboxChangeEvent) =>
    setSureToEdit(e.target.checked)
  const [editing, setEditing] = useState<string | undefined>()
  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const {
    task: { evaluationStepDefinitions, assignment }
  } = result.data

  const assignmentOpen = assignment?.status === 'OPEN'
  const settingsEditable = sureToEdit || !assignmentOpen

  const handlePositionChange = (
    definition: EvaluationStepDefinition,
    newPosition: number
  ) =>
    setEvaluationStepDefinitionPosition({
      variables: { id: definition.id, position: newPosition }
    }).then(() => messageService.success('Order updated'))

  const renderEvaluationStepDefinition = (
    definition: EvaluationStepDefinition
  ) => {
    const confirmDelete = () =>
      Modal.confirm({
        title: 'Are you sure?',
        width: 600,
        content: (
          <>
            <p>
              Do you want to delete this evaluation step? Custom configuration
              will be lost!
            </p>
            <Alert message="This will fail if the evaluation has already been run since this step was added. This is because it is referenced by the generated feedback. If you are in testing mode, exit it to delete your answer. You can always deactivate a step to exclude it from future evaluations." />
          </>
        ),
        async onOk() {
          try {
            await deleteStep({ variables: { id: definition.id } })
          } catch (e) {
            /* Close modal on error */
          }
        }
      })

    const cardProps: CardProps = {
      title: definition.title,
      extra: (
        <>
          <Tooltip title="Delete evaluation step">
            <Button
              style={{ marginRight: 8 }}
              onClick={confirmDelete}
              type="dashed"
              shape="circle"
              danger
              icon={<DeleteOutlined />}
            />
          </Tooltip>
          <Tooltip title="Edit evaluation step">
            <Button
              disabled={!settingsEditable}
              onClick={() => setEditing(definition.id)}
              style={{ marginRight: 8 }}
              shape="circle"
              icon={<SettingOutlined />}
            />
          </Tooltip>
        </>
      ),
      children: (
        <>
          <EditEvaluationStepDefinitionModal
            visible={editing === definition.id}
            initialValues={definition}
            onSave={async updatedDefinition => {
              // merge updated values and original values but leave out
              // key, __typename and position
              const { key, __typename, position, ...fullDefinition } = {
                ...definition,
                ...updatedDefinition
              }
              await updateMutation({ variables: { input: fullDefinition } })
              setEditing(undefined)
            }}
            onCancel={() => setEditing(undefined)}
          />
          {definition.reportPath && definition.reportFormat && (
            <p>
              Files found at <code>{definition.reportPath}</code> will be parsed
              with <code>{definition.reportFormat}</code>.
            </p>
          )}
          <SyntaxHighlighter language="bash">
            {definition.script}
          </SyntaxHighlighter>
        </>
      )
    }
    return cardProps
  }

  const onCreate = async () => {
    await createStep({ variables: { taskId } })
    messageService.success('Evaluation step added')
    const newSteps = (await result.refetch()).data.task
      .evaluationStepDefinitions
    if (newSteps.length) {
      setEditing(newSteps[newSteps.length - 1].id)
    }
  }

  return (
    <>
      <Alert
        message={
          <>
            Here you can configure the automatic evaluation. Only you as a
            teacher can see this. To try it out, enable testing mode and start
            the evaluation.
            <br />
            The order of evaluation steps determines the order of the feedback
            displayed to students.
          </>
        }
        style={{ marginBottom: 16 }}
      />
      {assignmentOpen ? (
        <Alert
          style={{ marginBottom: 16 }}
          message="Warning"
          description={
            <>
              The assignment is already open. If you make changes to the
              configuration, all evaluations are marked as out of date and must
              be re-run. <Checkbox onChange={onSureToEditChange} /> I understand
              this and want to do it anyway
            </>
          }
          type="warning"
          showIcon
        />
      ) : null}
      <CardList
        sortable
        items={evaluationStepDefinitions}
        itemKey={step => step.id}
        renderItem={renderEvaluationStepDefinition}
        handlePositionChange={handlePositionChange}
      />
      <div style={{ marginTop: 16, textAlign: 'center' }}>
        <Button onClick={onCreate} type="dashed" icon={<PlusOutlined />}>
          Add evaluation step
        </Button>
      </div>
    </>
  )
}

export default EditEvaluationPage
