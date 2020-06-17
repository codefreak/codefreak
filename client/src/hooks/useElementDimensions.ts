import { createRef, RefObject, useEffect, useState } from 'react'
import { debounce } from 'ts-debounce'

export interface ElementDimensions {
  offsetWidth: number
  offsetHeight: number
  clientWidth: number
  clientHeight: number
  scrollWidth: number
  scrollHeight: number
}

const getDimensions = (e: HTMLElement): ElementDimensions => {
  return {
    offsetWidth: e.offsetWidth,
    offsetHeight: e.offsetHeight,
    clientWidth: e.clientWidth,
    clientHeight: e.clientHeight,
    scrollWidth: e.scrollWidth,
    scrollHeight: e.scrollHeight
  }
}

const dimensionsChanged = (
  a: ElementDimensions,
  b: ElementDimensions
): boolean => {
  return (
    a.offsetWidth !== b.offsetWidth ||
    a.offsetHeight !== b.offsetHeight ||
    a.clientWidth !== b.clientWidth ||
    a.clientHeight !== b.clientHeight ||
    a.scrollWidth !== b.scrollWidth ||
    a.scrollHeight !== b.scrollHeight
  )
}

/**
 * Observe the various width/height properties of an HTMLElement.
 * The hook will change its state if element dimensions change or element changes.
 * Returns an array with a ref object that should be assigned to an element and the dimensions.
 */
const useElementDimensions = <T extends HTMLElement>(): [
  RefObject<T>,
  ElementDimensions | undefined
] => {
  const ref = createRef<T>()
  const [dimensions, setDimensions] = useState<ElementDimensions>()

  useEffect(() => {
    if (ref.current) {
      const currentContainer = ref.current
      const calculateDimensionChanges = () => {
        const newDimensions = getDimensions(currentContainer)
        if (!dimensions || dimensionsChanged(newDimensions, dimensions)) {
          setDimensions(newDimensions)
        }
      }
      calculateDimensionChanges()

      const onResize = debounce(calculateDimensionChanges, 100)
      window.addEventListener('resize', onResize)
      return () => {
        window.removeEventListener('resize', onResize)
      }
    }
  }, [ref, setDimensions, dimensions])

  return [ref, dimensions]
}

export default useElementDimensions
