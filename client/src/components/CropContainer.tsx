import { Button, Icon } from 'antd'
import React, { useCallback, useEffect, useRef, useState } from 'react'

import './CropContainer.less'

const CropContainer: React.FC<{ maxHeight: number }> = ({
  children,
  maxHeight
}) => {
  const [hasOverflow, setHasOverflow] = useState<boolean>(false)
  const [isExpanded, setIsExpanded] = useState<boolean>(false)
  const [height, setHeight] = useState<number>(maxHeight)
  const containerRef = useRef<HTMLDivElement>(null)

  // "children" is an important dependency for the following callback
  // even if it's not used directly, it influences the height of containerRef
  const syncHeight = useCallback(() => {
    if (containerRef.current) {
      const newHeight = isExpanded
        ? containerRef.current.scrollHeight
        : maxHeight
      setHeight(newHeight)
      setHasOverflow(containerRef.current.scrollHeight > maxHeight)
    } else {
      setHeight(maxHeight)
    }
  }, [maxHeight, children, containerRef, isExpanded, setHeight, setHasOverflow])

  useEffect(() => {
    syncHeight()
    window.addEventListener('resize', syncHeight)
    return () => {
      window.removeEventListener('resize', syncHeight)
    }
  }, [syncHeight])

  const onExpandClick = useCallback(() => {
    setIsExpanded(!isExpanded)
    syncHeight()
  }, [setIsExpanded, isExpanded, syncHeight])

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
              Show less <Icon type="caret-up" />
            </>
          ) : (
            <>
              Show more <Icon type="caret-down" />
            </>
          )}
        </Button>
      ) : null}
    </div>
  )
}

export default CropContainer
