import Button, { ButtonProps } from 'antd/lib/button'
import React from 'react'
import { Link } from 'react-router-dom'
import { Answer, Task } from '../services/codefreak-api'
import { shorten } from '../services/short-id'
import { CloudOutlined } from '@ant-design/icons'

interface IdeButtonProps extends ButtonProps {
  answer: Pick<Answer, 'ideRunning'>
  task: Pick<Task, 'id'>
}

const IdeButton: React.FC<IdeButtonProps> = props => {
  const { answer, task, ...restProps } = props

  return (
    <Link to={`/tasks/${shorten(task.id)}/answer/edit`}>
      <Button icon={<CloudOutlined />} {...restProps}>
        Open in online IDE{answer.ideRunning ? ' (running)' : ''}
      </Button>
    </Link>
  )
}

export default IdeButton
