import { Icon } from 'antd'
import { IconProps } from 'antd/es/icon'

const EvaluationProcessingIcon: React.FC<IconProps> = ({
  className,
  ...props
}) => {
  const classes = 'spin-slow ' + (className ? className : '')
  return <Icon type="setting" spin className={classes} {...props} />
}

export default EvaluationProcessingIcon
