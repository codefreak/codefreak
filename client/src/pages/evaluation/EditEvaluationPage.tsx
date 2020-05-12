import {
  Alert,
  Button,
  Descriptions,
  Modal,
  Radio,
  Switch,
  Tag,
  Tooltip
} from 'antd'
import { CardProps } from 'antd/lib/card'
import { RadioChangeEvent } from 'antd/lib/radio'
import YAML from 'json-to-pretty-yaml'
import React, { useState } from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import CardList from '../../components/CardList'
import SyntaxHighlighter from '../../components/code/SyntaxHighlighter'
import EditableTitle from '../../components/EditableTitle'
import {
  EvaluationRunner,
  EvaluationStepDefinitionInput,
  GetEvaluationStepDefinitionsQueryResult,
  useCreateEvaluationStepDefinitionMutation,
  useDeleteEvaluationStepDefinitionMutation,
  useGetEvaluationRunnersQuery,
  useGetEvaluationStepDefinitionsQuery,
  useSetEvaluationStepDefinitionPositonMutation,
  useUpdateEvaluationStepDefinitionMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { makeUpdater } from '../../services/util'

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
  const [
    setEvaluationStepDefinitionPosition
  ] = useSetEvaluationStepDefinitionPositonMutation()

  const [updateMutation] = useUpdateEvaluationStepDefinitionMutation({
    onCompleted: () => {
      result.refetch()
      messageService.success('Step updated')
    }
  })

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const {
    task: { evaluationStepDefinitions }
  } = result.data

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
    const definitionInput: EvaluationStepDefinitionInput = {
      id: definition.id,
      active: definition.active,
      title: definition.title,
      options: definition.options
    }

    const updater = makeUpdater(definitionInput, input =>
      updateMutation({ variables: { input } })
    )

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
      title: (
        <EditableTitle
          editable
          title={definition.title}
          onChange={updater('title')}
        />
      ),
      extra: (
        <>
          {definition.runner.builtIn ? null : (
            <Tooltip title="Delete evaluation step" placement="left">
              <Button
                onClick={confirmDelete}
                type="dashed"
                shape="circle"
                icon="delete"
              />
            </Tooltip>
          )}
        </>
      ),
      children: (
        <>
          <Descriptions layout="horizontal" style={{ marginBottom: -8 }}>
            <Descriptions.Item label="Runner">
              {definition.runnerName}{' '}
              {definition.runner.builtIn ? (
                <Tooltip
                  placement="right"
                  title="Built-in evaluation steps cannot be deleted. You can still hide them from students by deactivating."
                >
                  <Tag>built-in</Tag>
                </Tooltip>
              ) : null}
            </Descriptions.Item>
            <Descriptions.Item label="Active">
              <Tooltip
                placement="right"
                title="If disabled, this evaluation step will not be run in future evaluation. Feedback for this step is hidden from students existing evaluations."
              >
                <Switch
                  checked={definition.active}
                  onChange={updater('active')}
                />
              </Tooltip>
            </Descriptions.Item>
          </Descriptions>
          {definition.options === '{}' ? (
            <i>Default configuration</i>
          ) : (
            <SyntaxHighlighter language="yaml" noLineNumbers>
              {YAML.stringify(JSON.parse(definition.options))}
            </SyntaxHighlighter>
          )}
        </>
      )
    }
    return cardProps
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
      <CardList
        sortable
        items={evaluationStepDefinitions}
        renderItem={renderEvaluationStepDefinition}
        handlePositionChange={handlePositionChange}
      />
      <AddEvaluationStepDefinitionButton
        taskId={taskId}
        update={result.refetch}
      />
    </>
  )
}

const AddEvaluationStepDefinitionButton: React.FC<{
  taskId: string
  update: () => any
}> = props => {
  const [modalVisible, setModalVisible] = useState(false)
  const [runnerName, setRunnerName] = useState<string>()
  const showModal = () => {
    setRunnerName(undefined)
    setModalVisible(true)
  }
  const hideModal = () => setModalVisible(false)
  const result = useGetEvaluationRunnersQuery()
  const [createStep, { loading }] = useCreateEvaluationStepDefinitionMutation()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const runners = result.data.evaluationRunners

  const onChange = (e: RadioChangeEvent) => setRunnerName(e.target.value)

  const onOk = () =>
    createStep({
      variables: { runnerName: runnerName!!, taskId: props.taskId }
    })
      .then(() => messageService.success('Evaluation step added'))
      .then(props.update)
      .finally(hideModal)

  return (
    <>
      <Button
        onClick={showModal}
        type="primary"
        style={{ marginTop: 16 }}
        icon="plus"
        block
      >
        Add Evaluation Step
      </Button>
      <Modal
        title="Add Evaluation Step"
        visible={modalVisible}
        onOk={onOk}
        okButtonProps={{ disabled: runnerName === undefined, loading }}
        onCancel={hideModal}
      >
        <Radio.Group onChange={onChange} value={runnerName}>
          {runners.map(renderRunnerRadio)}
        </Radio.Group>
      </Modal>
    </>
  )
}

const renderRunnerRadio = (
  runner: Pick<EvaluationRunner, 'name' | 'defaultTitle'>
) => (
  <Radio
    style={{ display: 'block', height: '30px', lineHeight: '30px' }}
    value={runner.name}
    key={runner.name}
  >
    {runner.defaultTitle} <Tag>{runner.name}</Tag>
  </Radio>
)

export default EditEvaluationPage
