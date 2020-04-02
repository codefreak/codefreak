import React, { ImgHTMLAttributes } from 'react'

const Logo: React.FC<ImgHTMLAttributes<HTMLImageElement>> = props => {
  return (
    <img
      src={process.env.PUBLIC_URL + '/codefreak-logo.svg'}
      alt="Code FREAK Logo"
      title="Code FREAK"
      {...props}
    />
  )
}

export default Logo
