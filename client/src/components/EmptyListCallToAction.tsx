import React from 'react'

const EmptyListCallToAction: React.FC<{ title?: string }> = ({
  title,
  children
}) => {
  return (
    <div
      style={{ display: 'flex', alignItems: 'flex-start', paddingRight: 20 }}
    >
      <div style={{ flex: '1 1' }} />
      <div
        style={{
          textAlign: 'center',
          fontSize: '30px',
          paddingRight: 20,
          paddingTop: 85
        }}
      >
        {title ? (
          <>
            {title}
            <br />
          </>
        ) : null}
        <span style={{ fontSize: 30, color: '#001529' }}>{children}</span>
      </div>
      <img alt="" src={process.env.PUBLIC_URL + '/arrow-top-right.png'} />
    </div>
  )
}

export default EmptyListCallToAction
