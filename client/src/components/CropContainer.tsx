import { CaretDownOutlined, CaretUpOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import React, { useCallback, useEffect, useState } from 'react'
import useElementDimensions from '../hooks/useElementDimensions'

import './CropContainer.less'

const CropContainer: React.FC<{ maxHeight: number }> = ({
  children,
  maxHeight
}) => {
  const [hasOverflow, setHasOverflow] = useState<boolean>(false)
  const [isExpanded, setIsExpanded] = useState<boolean>(false)
  const [scrollHeight, setScrollHeight] = useState<number>(0)
  const [containerRef, dimensions] = useElementDimensions<HTMLDivElement>()

  const onExpandClick = useCallback(() => {
    setIsExpanded(!isExpanded)
  }, [setIsExpanded, isExpanded])

  useEffect(() => {
    if (dimensions) {
      setScrollHeight(dimensions.scrollHeight)
      setHasOverflow(dimensions.scrollHeight > maxHeight)
    }
  }, [setScrollHeight, setHasOverflow, maxHeight, dimensions])

  // set fixed max-height so animation work properly
  // by default we unset max-height
  let height: number | string = ''
  if (hasOverflow && scrollHeight) {
    height = isExpanded ? scrollHeight : maxHeight
  }
  return (
    <div
      className={`crop-container ${hasOverflow ? 'has-overflow' : ''} ${
        isExpanded ? 'expanded' : ''
      }`}
    >
      <div
        className="crop-container-body"
        style={{ maxHeight: height }}
        ref={containerRef}
      >
        {children}
      </div>
      {hasOverflow ? (
        <Button
          type="link"
          size="small"
          className="show-toggle"
          onClick={onExpandClick}
        >
          {isExpanded ? (
            <>
              Show less <CaretUpOutlined />
            </>
          ) : (
            <>
              Show more <CaretDownOutlined />
            </>
          )}
        </Button>
      ) : null}
    </div>
  )
}

export default CropContainer
