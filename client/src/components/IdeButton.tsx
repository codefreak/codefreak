import Button, { ButtonProps } from 'antd/lib/button'
import React from 'react'
import { Answer } from '../services/codefreak-api'
import { shorten } from '../services/short-id'

interface IdeButtonProps extends ButtonProps {
  answer: Pick<Answer, 'id' | 'ideRunning'>
}

const IdeButton: React.FC<IdeButtonProps> = props => {
  const { answer, ...restProps } = props

  return (
    <a href={'/ide/answer/' + shorten(answer.id)} target={answer.id}>
      <Button icon="cloud" {...restProps}>
        Open in online IDE{answer.ideRunning ? ' (running)' : ''}
      </Button>
    </a>
  )
}

export default IdeButton
