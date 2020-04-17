import Header, { HeaderViewProps } from '@ant-design/pro-layout/es/Header'
import React from 'react'
import DefaultLayout from './DefaultLayout'

// force the mobile header render without respecting breakpoints
const renderCompactHeader: React.FC<HeaderViewProps> = props => {
  const headerProps = {
    ...props,
    headerRender: undefined,
    isMobile: true
  }
  return <Header {...headerProps} />
}

/**
 * Layout without sidebar navigation
 */
const EmbeddedLayout: React.FC = ({ children }) => {
  return (
    <DefaultLayout menuRender={false} headerRender={renderCompactHeader}>
      {children}
    </DefaultLayout>
  )
}

export default EmbeddedLayout
