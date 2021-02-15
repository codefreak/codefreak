import './Points.less'
import React, {useState} from "react";
import {
  PointsOfEvaluationStep, PointsOfEvaluationStepInput,
  useGetPointsOfEvaluationStepByEvaluationStepIdQuery,
  useUpdatePointsOfEvaluationStepMutation
} from "../../generated/graphql";
import useHasAuthority from "../../hooks/useHasAuthority";
import {Empty, InputNumber, Switch} from "antd";
import {messageService} from "../../services/message";
import {debounce} from "ts-debounce";


const PointsEdit: React.FC<{
  evaluationStepId: string
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
      messageService.success('Points updated')
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
        if(debounce(updatePointsOfEvaluationStep({variables: {input}}).then(r => result.refetch()).then,200))return
        // updatePointsOfEvaluationStep({variables: {input}}).then(r => refetch())
        // return
      }
    }

    if(!data.pointsOfEvaluationStepByEvaluationStepId.gradeDefinitionMax.active)return (<div> </div>)

    if (auth) {
      // return renderEdit(data.poe!,onPoEStepChange,onEnabledChange,changeable)
      return (<React.Fragment>
        {renderEdit(data.pointsOfEvaluationStepByEvaluationStepId,onPoEStepChange,onEnabledChange,changeable)}
      </React.Fragment>)

    } else {
      // return renderView(data.poe!.pOfE, data.poe!.gradeDefinitionMax.pEvalMax)
      return (<React.Fragment>
        { renderView(data.pointsOfEvaluationStepByEvaluationStepId.pOfE, data.pointsOfEvaluationStepByEvaluationStepId!.gradeDefinitionMax.pEvalMax)}
      </React.Fragment>)
    }
  }else{
    return (<React.Fragment><Empty/></React.Fragment>)
  }
}


const renderView=(
  reachedPoints:number |undefined,
  maxPoints: number | undefined
)=>{
  return (<div className="points-view">{reachedPoints}/{maxPoints} Points</div>)
}

const renderEdit=(
  poe:PointsOfEvaluationStep,
  onChange:(value:number)=>void,
  onSwitch:(changeable:boolean)=>void,
  changeable : boolean
)=>{


  //InputNumber Config
  const onChangeDefinitely = (val: number | undefined) =>
    val !== undefined ? onChange(val) : undefined
  const parser = (val: string | undefined) => {
    if (!val) {
      return ''
    }
    return val.replace(/[^\d]+/, '')
  }
  const formatter = (val: string | number | undefined) => `${val}`

  const input =(    <InputNumber
    title={"Points"}
    min={0}
    max={poe.gradeDefinitionMax.pEvalMax}
    onChange={onChangeDefinitely}
    value={poe.pOfE}
    parser={parser}
    formatter={formatter}
    disabled={!changeable}
  />)

  const lever =(
    <Switch defaultChecked={changeable} onChange={onSwitch} />
    )

  return(<div className="points-edit">{lever}{input}</div>)

}

export default PointsEdit
