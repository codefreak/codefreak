import { InputNumber, Switch } from 'antd'
import { InputNumberProps } from 'antd/es/input-number'
import React, { useEffect, useMemo, useState } from 'react'
import './GradingDefinitionInputField.less'
import {
  GetGradingDefinitionDocument,
  GradingDefinition,
  GradingDefinitionInput,
  useUpdateGradingDefinitionMutation
} from '../../generated/graphql'
import HelpTooltip from '../HelpTooltip'
import { debounce } from 'ts-debounce'
import { valueType } from 'antd/es/statistic/utils'
import { messageService } from '../../services/message'

const renderGradePoints = (
  title: string,
  value: number,
  onChange: (value: number) => void,
  additionalProps: InputNumberProps = {}
) => {
  const onChangeValid = (val: valueType | undefined) => {
    if (typeof val === 'number') {
      onChange(val)
    }
  }

  return (
    <InputNumber
      title={title}
      min={0}
      onChange={onChangeValid}
      inputMode={'numeric'}
      value={value}
      {...additionalProps}
    />
  )
}

const renderGradeErrors = (
  title: string,
  value: number,
  max: number,
  onChange: (value: number) => void,
  additionalProps: InputNumberProps = {}
) => {
  const onChangeValid = (val: valueType | undefined) => {
    if (typeof val === 'number') {
      onChange(val)
    }
  }

  return (
    <InputNumber
      title={title}
      min={0}
      max={max}
      onChange={onChangeValid}
      inputMode={'numeric'}
      value={value}
      {...additionalProps}
    />
  )
}

const GradingDefinitionInputField: React.FC<{
  gradingDefinition: GradingDefinition
  disable: boolean
}> = props => {
  const [updateState, setUpdateState] = useState<boolean>(false)

  const [
    gradingDefinitionResult,
    setGradingDefinition
  ] = useState<GradingDefinition>(props.gradingDefinition)

  const gradingDefinition = gradingDefinitionResult.id

  const [updateMutation] = useUpdateGradingDefinitionMutation({
    onCompleted: () => {
      messageService.success('Grading Definition Updated')
    },
    refetchQueries: [
      {
        query: GetGradingDefinitionDocument,
        variables: { gradingDefinition }
      }
    ]
  })

  const [
    gradingDefinitionInput,
    setGradeDefinitionInput
  ] = useState<GradingDefinitionInput>()

  useEffect(() => {
    setGradeDefinitionInput({
      criticalMistakePenalty: gradingDefinitionResult.criticalMistakePenalty,
      majorMistakePenalty: gradingDefinitionResult.majorMistakePenalty,
      minorMistakePenalty: gradingDefinitionResult.minorMistakePenalty,
      id: gradingDefinitionResult.id,
      maxPoints: gradingDefinitionResult.maxPoints
    })
  }, [gradingDefinitionResult])

  // debounces user input by only updating after 500ms have passed
  const debounceMutation = useMemo(
    () =>
      debounce((input: GradingDefinitionInput) => {
        updateMutation({ variables: input }).then()
      }, 500),
    [updateMutation]
  )

  // updateState prevents this effect from being loaded on rendering
  useEffect(() => {
    if (updateState) {
      if (gradingDefinitionInput !== undefined) {
        debounceMutation(gradingDefinitionInput).then()
      }
    }
  }, [debounceMutation, gradingDefinitionInput, updateState])

  const [changeable, setChangeable] = useState<boolean>(false)

  const onEnabledChange = (state: boolean) => {
    setChangeable(state)
  }

  const createOnValueChange = (field: keyof GradingDefinitionInput) => (
    value: number
  ) => {
    const valueOfKey = { ...gradingDefinitionResult, [field]: value }
    if (field !== null && gradingDefinitionInput !== null) {
      setUpdateState(true)
      setGradeDefinitionInput(valueOfKey)
      setGradingDefinition(valueOfKey)
    }
  }

  return (
    <div className="grading-definition-input">
      <div className="max-Points-input">
        {renderGradePoints(
          'Max Points',
          gradingDefinitionResult.maxPoints,
          createOnValueChange('maxPoints'),
          {
            disabled: !changeable
          }
        )}
      </div>
      <div className="max-Points">Max-Points</div>
      <div className="minor-mistake-penalty-input">
        {renderGradeErrors(
          'Minor Mistake Penalty',
          gradingDefinitionResult.minorMistakePenalty,
          gradingDefinitionResult.maxPoints,
          createOnValueChange('minorMistakePenalty'),
          {
            disabled: !changeable
          }
        )}
      </div>
      <div className="minor-mistake-penalty">Minor-Error</div>
      <div className="major-mistake-penalty-input">
        {renderGradeErrors(
          'Major Mistake Penalty',
          gradingDefinitionResult.majorMistakePenalty,
          gradingDefinitionResult.maxPoints,
          createOnValueChange('majorMistakePenalty'),
          {
            disabled: !changeable
          }
        )}
      </div>
      <div className="major-mistake-penalty">Major-Error</div>
      <div className="critical-mistake-penalty-input">
        {renderGradeErrors(
          'Critical Mistake Penalty',
          gradingDefinitionResult.criticalMistakePenalty,
          gradingDefinitionResult.maxPoints,
          createOnValueChange('criticalMistakePenalty'),
          {
            disabled: !changeable
          }
        )}
      </div>
      <div className="critical-mistake-penalty">Critical-Error</div>
      <div className="grading-definition-unlock">
        <Switch
          defaultChecked={changeable}
          onChange={onEnabledChange}
          disabled={props.disable}
        />
        <HelpTooltip placement="top" title="unlock fields" />
      </div>
    </div>
  )
}

export default GradingDefinitionInputField
