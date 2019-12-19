import React from 'react'

const Centered: React.FC<{
  style?: React.CSSProperties
  className?: string
}> = props => {
  const style = {
    height: '100%',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    ...props.style
  }
  return (
    <div style={style} className={props.className}>
      {props.children}
    </div>
  )
}

export default Centered
