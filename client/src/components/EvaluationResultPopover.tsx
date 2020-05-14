import { List, Popover } from 'antd'
import React from 'react'
import {
  EvaluationStep,
  PublicUserFieldsFragment,
  Task
} from '../generated/graphql'
import { displayName } from '../services/user'
import EvaluationStepResultIcon from './EvaluationStepResultIcon'

import './EvaluationResultPopover.less'

const EvaluationResultPopover: React.FC<{
  task: Pick<Task, 'title'>
  user: PublicUserFieldsFragment
  steps: Array<
    Pick<EvaluationStep, 'id' | 'summary' | 'result'> & {
      definition: { title: string }
    }
  >
}> = ({ task, user, steps, children }) => {
  const popoverContent = (
    <List itemLayout="horizontal" size="small">
      {steps.map(step => {
        return (
          <List.Item key={step.id}>
            <List.Item.Meta
              avatar={<EvaluationStepResultIcon stepResult={step.result} />}
              title={step.definition.title}
              description={step.summary}
            />
          </List.Item>
        )
      })}
    </List>
  )

  return (
    <Popover
      title={`${displayName(user)} / ${task.title}`}
      content={popoverContent}
      overlayClassName="evaluation-result-popover"
      arrowPointAtCenter
    >
      {children}
    </Popover>
  )
}

export default EvaluationResultPopover
