import {Descriptions, InputNumber, Switch} from 'antd'
import { InputNumberProps } from 'antd/es/input-number'
import React, { useState } from 'react'
import './GradeDefinitionInputField.less'
import {
  GradeDefinition,
  GradeDefinitionInput,
  useUpdateEvaluationStepDefinitionMutation, useUpdateGradeDefinitionValuesMutation
} from "../../generated/graphql";
import HelpTooltip from '../HelpTooltip';
import {messageService} from "../../services/message";

const renderGradeDefinitionInput =(
  title: string,
  value: number,
  onChange : (value: number) => void,
  additionalProps: InputNumberProps = {}
) =>{
  const onChangeValid = (val: number | undefined) =>
    val !== undefined ? onChange(val) : undefined

  const parser = (val: string | undefined) => {
    if (!val) {
      return ''
    }
    return val.replace(/[^\d]+/, '')
  }
    const formatter = (val: string | number | undefined) => `${val}`

    return (
      <InputNumber
        title={title}
        min={0}
        onChange={onChangeValid}
        value={value}
        parser={parser}
        formatter={formatter}
        {...additionalProps}
      />
    )
}

  /**
   * Standard configuration for Input
   */
// export interface GradeDefinitionInputProps{
//   defaultValue?: GradeComponents
//   placeholder?: GradeComponents
//   onChange?: (newComponents: GradeComponents | undefined) => void
//   // nullable?: boolean
// }

/**
 * Inputform to Enter and Mutate a GradeDefinition
 *
 */
const GradeDefinitionInputField: React.FC<{
  // defaultValue?: GradeComponents,
  // placeholder?: GradeComponents,
  // onChange?:(newComponents: GradeComponents | undefined) => void
  gradeDefinition: GradeDefinition
  fetchForUpdate:any
  disable: boolean
}> =props => {

  const [updateMutation] = useUpdateGradeDefinitionValuesMutation({
    onCompleted: () => {
      props.fetchForUpdate()
      messageService.success('GradeDefinition Value Updated')
    }
  })
  const [gradeDefinition,setGradeDefinition]=useState<GradeDefinition>(
    props.gradeDefinition || { pEvalMax: 0, bOnMinor: 0, bOnMajor: 0, bOnCritical: 0}
  )

  const [changeable,setChangeable] = useState<boolean>(
    false
  )

  const onEnabledChange = (state: boolean) => {
    setChangeable(state)
  }

  // if(props.gradeDefinition==null)return (<></>)
  // setGradeDefinition(props.gradeDefinition)

  const gradeDefinitionInput: GradeDefinitionInput ={
    id: gradeDefinition.id,
    pEvalMax: gradeDefinition.pEvalMax,
    bOnMinor: gradeDefinition.bOnMinor,
    bOnMajor: gradeDefinition.bOnMajor,
    bOnCritical: gradeDefinition.bOnCritical
  }

  const onGradeDefinitionChange = () => {
    gradeDefinitionInput.pEvalMax = gradeDefinition.pEvalMax
    gradeDefinitionInput.bOnMinor = gradeDefinition.bOnMinor
    gradeDefinitionInput.bOnMajor = gradeDefinition.bOnMajor
    gradeDefinitionInput.bOnCritical = gradeDefinition.bOnCritical
    console.log("onGradeDefinitionChange")
    printObject()
    return updateMutation({variables: {gradeDefinitionInput}})
  }




  const createOnValueChange = (field: keyof GradeDefinition) => (
    value: number
  ) => {
    const valueOfKey = {...gradeDefinition, [field]: value}
    //TODO Hier sind der State-Hook und ich unterschiedlicher Meinung. Irrationales verhalten
    console.log("createOnValueChange with value " + valueOfKey.pEvalMax + " and number " + value)
    printObject()
    setGradeDefinition(valueOfKey)
    //Man siehe in der Console, obwohl der Key geupdated wird, wird ers dann doch nicht. :D
    printObject()
    onGradeDefinitionChange()
  }

  const printObject = () =>{
    console.log("gradeDefinition -> " +
      " pEvalMax: " + gradeDefinition.pEvalMax,
      " bOnMinor: " + gradeDefinition.bOnMinor,
      " bOnMajor: " + gradeDefinition.bOnMajor,
      " bOnCritical:" + gradeDefinition.bOnCritical)
  }


  return (
    <div className="grade-definition-input">

      <div className="grade-definition-input-numbers">

          {renderGradeDefinitionInput("Max-Points", gradeDefinition.pEvalMax, createOnValueChange('pEvalMax'), {
            disabled: !changeable
          })}
          {renderGradeDefinitionInput("Minor-Error", gradeDefinition.bOnMinor, createOnValueChange('bOnMinor'), {
            disabled: !changeable
          })}
          {renderGradeDefinitionInput("Major-Error", gradeDefinition.bOnMajor, createOnValueChange('bOnMajor'), {
          disabled: !changeable
          })}
          {renderGradeDefinitionInput("Critical-Error", gradeDefinition.bOnCritical, createOnValueChange('bOnCritical'), {
          disabled: !changeable
          })}

      </div>
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
