import React, { createContext, useContext, useState } from 'react'
import { useQueryParam } from './useQuery'

const HideNavigationContext = createContext(false)

export const HIDE_NAVIGATION_QUERY_PARAM = 'hideNavigation'

export const useHideNavigation = () => useContext(HideNavigationContext)

export const HideNavigationProvider: React.FC = props => {
  const [hideNavigation] = useState(
    useQueryParam(HIDE_NAVIGATION_QUERY_PARAM) === 'true'
  )
  return (
    <HideNavigationContext.Provider value={hideNavigation}>
      {props.children}
    </HideNavigationContext.Provider>
  )
}
