import './Points.less'
import React, {useState} from "react";
import {
  PointsOfEvaluationStep, PointsOfEvaluationStepInput,
  useGetPointsOfEvaluationStepByEvaluationStepIdQuery,
  useUpdatePointsOfEvaluationStepMutation
} from "../../generated/graphql";
import useHasAuthority from "../../hooks/useHasAuthority";
import {Empty, InputNumber, Switch} from "antd";
import {debounce} from "ts-debounce";


const PointsEdit: React.FC<{
  evaluationStepId: string
  fetchGrade : any
}> = props=> {

  const evaluationStepId = props.evaluationStepId

  const result = useGetPointsOfEvaluationStepByEvaluationStepIdQuery({
    variables: {evaluationStepId}
  })

  const [changeable, setChangeable] = useState<boolean>(false)

  const onEnabledChange = (state: boolean) => {
    setChangeable(state)
  }
  const [updatePointsOfEvaluationStep] = useUpdatePointsOfEvaluationStepMutation({
    onCompleted:()=>{
      result.refetch()
      props.fetchGrade()
    }
  })


  const auth = useHasAuthority('ROLE_TEACHER')
  /**
   * Output
   */
  if(result.data != null){
    const data = result.data
    const input : PointsOfEvaluationStepInput={
      bOfT: data.pointsOfEvaluationStepByEvaluationStepId.bOfT,
      calcCheck: data.pointsOfEvaluationStepByEvaluationStepId.calcCheck,
      edited: data.pointsOfEvaluationStepByEvaluationStepId.edited,
      id: data.pointsOfEvaluationStepByEvaluationStepId.id,
      pOfE: data.pointsOfEvaluationStepByEvaluationStepId.pOfE,
      resultCheck: data.pointsOfEvaluationStepByEvaluationStepId.resultCheck
    }

    if(result.error) return (<div>an error occurred on </div>)

    const onPoEStepChange = (value : number) => {
      if(value!==undefined){
        console.log("value is: " + value)
        input.pOfE = value
        input.edited = true
        debounce(updatePointsOfEvaluationStep({variables: {input}}).then,500)

      }
    }
    if(data.pointsOfEvaluationStepByEvaluationStepId.gradeDefinitionMax==null){
      return (<div></div>)
    }else{
      if(!data.pointsOfEvaluationStepByEvaluationStepId.gradeDefinitionMax.active)return (<div> </div>)
    }


    if (auth) {
      return (renderEdit({
        poe:data.pointsOfEvaluationStepByEvaluationStepId,
        onChange:onPoEStepChange,
        onSwitch:onEnabledChange,
        changeable:changeable
      }))
    } else {
      return (renderView({
        reachedPoints:data.pointsOfEvaluationStepByEvaluationStepId.pOfE,
        maxPoints:data.pointsOfEvaluationStepByEvaluationStepId.gradeDefinitionMax.pEvalMax
      }))
    }
  }else{
    return (<Empty/>)
  }
}


const renderView : React.FC<{
  reachedPoints: number,
  maxPoints: number | undefined
}>=props=>{
  return (<div className="points-view">{props.reachedPoints}/{props.maxPoints} Points</div>)
}

const renderEdit : React.FC<{
    poe:PointsOfEvaluationStep,
    onChange:(value:number)=>void,
    onSwitch:(changeable:boolean)=>void,
    changeable : boolean
}>=props=>{

  const onChangeDefinitely = (val: number | undefined) =>{
    if(val!==undefined){
      if(val>props.poe.gradeDefinitionMax.pEvalMax ){
        props.onChange(props.poe.gradeDefinitionMax.pEvalMax)
      }else{
        props.onChange(val)
      }
    }
    // val !== undefined ? (val>props.poe.gradeDefinitionMax.pEvalMax ? props.onChange(props.poe.gradeDefinitionMax.pEvalMax) : props.onChange(val)) : undefined

  }
  const parser = (val: string | undefined) => {
    if (!val) {
      return '0'
    }
    return val.replace(/[^\d]+/, '0')
  }
  const formatter = (val: string | number | undefined) => `${val}`
  if(props.poe.gradeDefinitionMax.pEvalMax==0)return(<></>)

  const input =(    <InputNumber
    title={"Points"}
    min={0}
    max={props.poe.gradeDefinitionMax.pEvalMax}
    onChange={onChangeDefinitely}
    value={props.poe.pOfE}
    parser={parser}
    formatter={formatter}
    disabled={!props.changeable}
  />)

  const lever =(
    <Switch defaultChecked={props.changeable} onChange={props.onSwitch} />
    )

  return(<div className="points-edit">{lever}{input} / {props.poe.gradeDefinitionMax.pEvalMax}</div>)

}

export default PointsEdit
