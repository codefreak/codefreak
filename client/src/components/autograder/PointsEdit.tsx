import './Points.less'
import React, { useEffect, useMemo, useState } from 'react'
import {
  GetPointsOfEvaluationStepByEvaluationStepIdDocument,
  GradingDefinition,
  PointsOfEvaluationStep,
  PointsOfEvaluationStepInput,
  useGetPointsOfEvaluationStepByEvaluationStepIdQuery,
  useUpdatePointsOfEvaluationStepMutation
} from '../../generated/graphql'
import { InputNumber, Switch } from 'antd'
import { debounce } from 'ts-debounce'
import { messageService } from '../../services/message'

const PointsEdit: React.FC<{
  evaluationStepId: string
  fetchGrade: () => void
  teacherAuthority?: boolean
}> = props => {
  const evaluationStepId = props.evaluationStepId
  const auth = props.teacherAuthority

  const result = useGetPointsOfEvaluationStepByEvaluationStepIdQuery({
    variables: { evaluationStepId }
  })
  const data = result.data?.pointsOfEvaluationStepByEvaluationStepId

  const [updateState, setUpdateState] = useState<boolean>(false)

  const [updateMutation] = useUpdatePointsOfEvaluationStepMutation({
    onCompleted: () => {
      messageService.success('Points updated')
      props.fetchGrade()
    },
    refetchQueries: [
      {
        query: GetPointsOfEvaluationStepByEvaluationStepIdDocument,
        variables: { evaluationStepId }
      }
    ]
  })

  const [updatePoints, setUpdatePoints] = useState<boolean>(true)

  const [points, setPoints] = useState<PointsOfEvaluationStep>({
    reachedPoints: 0,
    mistakePoints: 0,
    edited: false,
    calculationCheck: false,
    evaluationStepResultCheck: false,
    id: '0',
    gradingDefinition: null
  })

  useEffect(() => {
    if (data !== undefined) {
      if (updatePoints) {
        setPoints({
          id: data.id,
          edited: data.edited,
          mistakePoints: data.mistakePoints,
          reachedPoints: data.reachedPoints,
          evaluationStepResultCheck: data.evaluationStepResultCheck,
          calculationCheck: data.calculationCheck,
          gradingDefinition: data.gradingDefinition as GradingDefinition
        })
        setUpdatePoints(false)
      }
    }
  }, [data, updatePoints, setPoints, setUpdatePoints])

  const [pointsInput, setPointsInput] = useState<
    PointsOfEvaluationStepInput | undefined
  >(undefined)

  useEffect(() => {
    if (points !== undefined) {
      setPointsInput({
        id: points.id!!,
        edited: points.edited!!,
        mistakePoints: points.mistakePoints!!,
        reachedPoints: points.reachedPoints!!,
        evaluationStepResultCheck: points.evaluationStepResultCheck!!,
        calculationCheck: points.calculationCheck!!
      })
    }
  }, [points])

  const debounceMutation = useMemo(
    () =>
      debounce((input: PointsOfEvaluationStepInput) => {
        updateMutation({ variables: { input } }).then()
      }, 500),
    [updateMutation]
  )

  useEffect(() => {
    if (updateState) {
      if (pointsInput !== undefined) {
        debounceMutation(pointsInput).then()
      }
    }
  }, [debounceMutation, pointsInput, updateState])

  const [editable, setEditable] = useState<boolean>(false)

  const onEnabledChange = (state: boolean) => {
    setEditable(state)
  }
  const createOnValueChange = (field: keyof PointsOfEvaluationStepInput) => (
    value: number
  ) => {
    const valueOfKey = { ...points, [field]: value }
    if (field !== null && pointsInput !== null) {
      setUpdateState(true)
      setPointsInput(valueOfKey as PointsOfEvaluationStepInput)
      setPoints(valueOfKey)
    }
  }

  if (points !== undefined && points.gradingDefinition !== undefined) {
    if (result.error) return <div>{result.error.message}</div>

    if (points.gradingDefinition === null) {
      return null
    } else if (!points.gradingDefinition!.active) {
      return null
    }
    if (auth) {
      return renderEdit(
        points.gradingDefinition,
        points,
        createOnValueChange('reachedPoints'),
        onEnabledChange,
        editable
      )
    } else {
      return renderView(
        points.reachedPoints!,
        points.gradingDefinition!.maxPoints
      )
    }
  } else {
    return null
  }
}

const renderView = (reachedPoints: number, maxPoints: number | undefined) => {
  return (
    <div className="points-view">
      {reachedPoints}/{maxPoints} Points
    </div>
  )
}

const renderEdit = (
  gradeDefinition: Pick<GradingDefinition, 'active' | 'maxPoints'>,
  pointsOfEvaluationStep: Pick<PointsOfEvaluationStep, 'reachedPoints'>,
  onChange: (value: number) => void,
  onSwitch: (changeable: boolean) => void,
  editable: boolean
) => {
  const onChangeDefinitely = (val: number | undefined) => {
    if (typeof val === 'number') {
      if (val > gradeDefinition.maxPoints) {
        onChange(gradeDefinition.maxPoints)
      } else {
        onChange(val)
      }
    }
  }

  if (gradeDefinition.maxPoints === 0) return <></>

  const input = (
    <InputNumber
      title={'Points'}
      min={0}
      max={gradeDefinition.maxPoints}
      onChange={onChangeDefinitely}
      inputMode={'numeric'}
      value={pointsOfEvaluationStep.reachedPoints!!}
      disabled={!editable}
    />
  )

  const lever = <Switch defaultChecked={editable} onChange={onSwitch} />

  return (
    <div className="points-edit">
      {lever}
      {input} / {gradeDefinition.maxPoints}
    </div>
  )
}

export default PointsEdit
