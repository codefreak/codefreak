import React, { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

const ScrollToHash: React.FC = () => {
  const location = useLocation()
  useEffect(() => {
    const element = document.getElementById(location.hash.replace('#', ''))
    setTimeout(() => {
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' })
      } else {
        window.scrollTo({
          behavior: 'auto',
          top: 0
        })
      }
    }, 100)
  }, [location])
  return null
}

export default ScrollToHash
