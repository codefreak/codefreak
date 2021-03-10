import {Descriptions, InputNumber, Switch, Typography} from 'antd'
import { InputNumberProps } from 'antd/es/input-number'
import React, {useEffect, useState} from 'react'
import './GradeDefinitionInputField.less'
import {
  GradeDefinition,
  GradeDefinitionInput,
  useUpdateEvaluationStepDefinitionMutation, useUpdateGradeDefinitionValuesMutation
} from "../../generated/graphql";
import HelpTooltip from '../HelpTooltip';
import {messageService} from "../../services/message";
import {makeUpdater} from "../../services/util";
import {debounce} from "ts-debounce";
import Title from "antd/es/typography/Title";

const renderGradePoints =(
  title: string,
  value: number,
  onChange : (value: number) => void,
  additionalProps: InputNumberProps = {}
) =>{
  const onChangeValid = (val: number | undefined) =>
    val !== undefined ? onChange(val) : undefined

  const parser = (val: string | undefined) => {
    if (!val) {
      return '0'
    }
    //(?:\d+(?:\.\d*)?|\.\d+) <- für Float
    ///[^\d]+/ <- decimal

    return val.replace(/[^\d]+/, '0')
  }
    const formatter = (val: string | number | undefined) => `${val}`

    return (
      <InputNumber
        title={title}
        min={0}
        onChange={onChangeValid}
        value={value}
        inputMode={"numeric"}
        {...additionalProps}
      />
    )
}

const renderGradeErrors =(
  title: string,
  value: number,
  max: number,
  onChange : (value: number) => void,
  additionalProps: InputNumberProps = {}
) =>{
  const onChangeValid = (val: number | undefined) =>
    val !== undefined ? onChange(val) : undefined

  const parser = (val: string | undefined) => {
    if (!val) {
      return '0'
    }
    //(?:\d+(?:\.\d*)?|\.\d+) <- für Float
    ///[^\d]+/ <- decimal
    return val.replace(/[^\d]+/, '0')
  }
  const formatter = (val: string | number | undefined) => `${val}`

  return (
    <InputNumber
      title={title}
      min={0}
      max={max}
      onChange={onChangeValid}
      value={value}
      parser={parser}
      formatter={formatter}
      {...additionalProps}
    />
  )
}


/**
 * Inputform to Enter and Mutate a GradeDefinition
 *
 */
const GradeDefinitionInputField: React.FC<{
  gradeDefinition: GradeDefinition
  fetchForUpdate: any
  disable: boolean
}> =props => {

  const [updateMutation] = useUpdateGradeDefinitionValuesMutation({
    onCompleted: () => {
      props.fetchForUpdate()
    }
  })

  const [globalField, setglobalField] = useState<keyof GradeDefinitionInput | null>(null)


  const [gradeDefinition, setGradeDefinition] = useState<GradeDefinition>(
    props.gradeDefinition || {pEvalMax: 0, bOnMinor: 0, bOnMajor: 0, bOnCritical: 0}
  )

  const input: GradeDefinitionInput = {
    id: gradeDefinition.id,
    pEvalMax: gradeDefinition.pEvalMax,
    bOnMinor: gradeDefinition.bOnMinor,
    bOnMajor: gradeDefinition.bOnMajor,
    bOnCritical: gradeDefinition.bOnCritical
  }

  useEffect(() => {
    if (globalField != null) {
      debounce(updateMutation({variables: {input}}).then, 500)
    }
  }, [gradeDefinition])

  const [changeable, setChangeable] = useState<boolean>(
    false
  )

  const onEnabledChange = (state: boolean) => {
    setChangeable(state)
  }

  const updater = makeUpdater(input, input =>
    updateMutation({variables: {input}})
  )


  const createOnValueChange = (field: keyof GradeDefinitionInput) => (
    value: number
  ) => {
    const valueOfKey = {...gradeDefinition, [field]: value}
    if (typeof value == "number") {
      setglobalField(field)
      setGradeDefinition(valueOfKey)
    } else {
    }
  }

  const printObject = () => {
    console.log("gradeDefinition -> " +
      " pEvalMax: " + gradeDefinition.pEvalMax,
      " bOnMinor: " + gradeDefinition.bOnMinor,
      " bOnMajor: " + gradeDefinition.bOnMajor,
      " bOnCritical:" + gradeDefinition.bOnCritical)
  }


  return (
    <div className="grade-definition-input">
      <div className="maxPointsInput">
        {renderGradePoints("Max-Points", gradeDefinition.pEvalMax, createOnValueChange('pEvalMax'), {
          disabled: !changeable
        })}</div>
      <div className="maxPoints">Max-Points</div>
      <div className="minorErrorInput">
        {renderGradeErrors("Minor-Error", gradeDefinition.bOnMinor, gradeDefinition.pEvalMax, createOnValueChange('bOnMinor'), {
          disabled: !changeable
        })}
      </div>
      <div className="minorError">Minor-Error</div>
      <div className="majorErrorInput">
        {renderGradeErrors("Major-Error", gradeDefinition.bOnMajor, gradeDefinition.pEvalMax, createOnValueChange('bOnMajor'), {
          disabled: !changeable
        })}
      </div>
      <div className="majorError">Major-Error</div>
      <div className="criticalErrorInput">
        {renderGradeErrors("Critical-Error", gradeDefinition.bOnCritical, gradeDefinition.pEvalMax, createOnValueChange('bOnCritical'), {
          disabled: !changeable
        })}
      </div>
      <div className="criticalError">Critical-Error</div>
      <div className="grade-definition-unlock">
        <Switch defaultChecked={changeable} onChange={onEnabledChange} disabled={props.disable}/>
        <HelpTooltip
          placement="top"
          title="unlock fields"
        />
      </div>

    </div>
  )
}

export default GradeDefinitionInputField
