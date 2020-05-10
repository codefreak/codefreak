import { Alert, Button, Descriptions, Modal, Tag, Tooltip } from 'antd'
import { CardProps } from 'antd/lib/card'
import YAML from 'json-to-pretty-yaml'
import React from 'react'
import AsyncPlaceholder from '../../components/AsyncContainer'
import CardList from '../../components/CardList'
import SyntaxHighlighter from '../../components/code/SyntaxHighlighter'
import {
  GetEvaluationStepDefinitionsQueryResult,
  useDeleteEvaluationStepDefinitionMutation,
  useGetEvaluationStepDefinitionsQuery,
  useSetEvaluationStepDefinitionPositonMutation
} from '../../services/codefreak-api'
import { messageService } from '../../services/message'

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
    const confirmDelete = () =>
      Modal.confirm({
        title: 'Are you sure?',
        content:
          'Do you want to delete this evaluation step? Custom configuration will be lost!',
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
    </>
  )
}

export default EditEvaluationPage
