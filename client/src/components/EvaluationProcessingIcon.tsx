import { Icon } from 'antd'
import { IconProps } from 'antd/es/icon'

interface EvaluationProcessingIconProps extends IconProps {}

const EvaluationProcessingIcon: React.FC<EvaluationProcessingIconProps> = ({
  className,
  ...props
}) => {
  const classes = 'spin-slow ' + (className ? className : '')
  return <Icon type="setting" spin className={classes} {...props} />
}

export default EvaluationProcessingIcon
