import { Alert, Button, Descriptions, Modal, Switch, Tag, Tooltip } from 'antd'
import { ButtonProps } from 'antd/lib/button'
import { CardProps } from 'antd/lib/card'
import { JSONSchema6 } from 'json-schema'
import YAML from 'json-to-pretty-yaml'
import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import CardList from '../../components/CardList'
import SyntaxHighlighter from '../../components/code/SyntaxHighlighter'
import EditableTitle from '../../components/EditableTitle'
import JsonSchemaEditButton from '../../components/JsonSchemaEditButton'
import {
  EvaluationRunner,
  EvaluationStepDefinitionInput,
  GetEvaluationStepDefinitionsQueryResult,
  useCreateEvaluationStepDefinitionMutation,
  useDeleteEvaluationStepDefinitionMutation,
  useGetEvaluationStepDefinitionsQuery,
  useSetEvaluationStepDefinitionPositonMutation,
  useUpdateEvaluationStepDefinitionMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'
import { makeUpdater } from '../../services/util'

type EvaluationStepDefinition = NonNullable<
  GetEvaluationStepDefinitionsQueryResult['data']
>['task']['evaluationStepDefinitions'][0]

const parseSchema = (schema: string) => {
  const optionsSchema: JSONSchema6 = JSON.parse(schema)
  const hasProperties =
    optionsSchema.properties && Object.keys(optionsSchema.properties).length > 0
  return { optionsSchema, hasProperties }
}

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

  const [createStep] = useCreateEvaluationStepDefinitionMutation()

  if (result.data === undefined) {
    return <AsyncPlaceholder result={result} />
  }

  const {
    task: { evaluationStepDefinitions },
    evaluationRunners
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

    const runner = evaluationRunners.find(
      r => r.name === definition.runnerName
    )!
    const { optionsSchema, hasProperties } = parseSchema(runner.optionsSchema)

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

    const updateOptions = (newOptions: any) =>
      updater('options')(JSON.stringify(newOptions))

    const configureButtonProps: ButtonProps = {
      type: 'primary',
      shape: 'circle',
      icon: 'setting'
    }

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
          {runner.builtIn ? null : (
            <Tooltip title="Delete evaluation step" placement="left">
              <Button
                style={{ marginRight: 8 }}
                onClick={confirmDelete}
                type="dashed"
                shape="circle"
                icon="delete"
              />
            </Tooltip>
          )}
          {hasProperties ? (
            <JsonSchemaEditButton
              title={`Configure ${definition.runnerName} step`}
              value={JSON.parse(definition.options)}
              schema={optionsSchema}
              onSubmit={updateOptions}
              buttonProps={configureButtonProps}
            />
          ) : (
            <Tooltip
              placement="left"
              title="This runner does not have any configuration options"
            >
              <Button disabled {...configureButtonProps} />
            </Tooltip>
          )}
        </>
      ),
      children: (
        <>
          <Descriptions layout="horizontal" style={{ marginBottom: -8 }}>
            <Descriptions.Item label="Runner">
              {definition.runnerName}{' '}
              {runner.builtIn ? (
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

  const addableRunners = evaluationRunners.filter(r => !r.builtIn)

  const onCreate = (runnerName: string, options: string) =>
    createStep({ variables: { taskId, runnerName, options } }).then(() => {
      messageService.success('Evaluation step added')
      result.refetch()
    })

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
      <div style={{ marginTop: 16, textAlign: 'center' }}>
        <h3>Add Evaluation Step</h3>
        {addableRunners.map(renderAddStepButton(onCreate))}
      </div>
    </>
  )
}

const renderAddStepButton = (
  onCreate: (runnerName: string, options: string) => Promise<unknown>
) => (
  runner: Pick<EvaluationRunner, 'name' | 'defaultTitle' | 'optionsSchema'>
) => {
  const { optionsSchema, hasProperties } = parseSchema(runner.optionsSchema)
  const buttonProps: ButtonProps = {
    type: 'dashed',
    icon: 'plus',
    children: runner.defaultTitle,
    style: { margin: '0 4px' }
  }
  const createStepWithoutOptions = () => createStep({})
  const createStep = (options: unknown) =>
    onCreate(runner.name, JSON.stringify(options))
  if (!hasProperties) {
    return <Button {...buttonProps} onClick={createStepWithoutOptions} />
  }
  return (
    <JsonSchemaEditButton
      title={`Configure ${runner.name} step`}
      value={{}}
      schema={optionsSchema}
      onSubmit={createStep}
      buttonProps={buttonProps}
    />
  )
}

export default EditEvaluationPage
