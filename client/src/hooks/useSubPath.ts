import { useHistory, useLocation, useRouteMatch } from 'react-router'

/**
 * Control the path relative to the current route
 * @param replace when setting the subPath, should it be replaced or pushed in the history
 * @param deep when getting the subPath, include all levels below the current (true) or only the first level (false)
 */
const useSubPath = (replace: boolean = true, deep: boolean = false) => {
  const { url } = useRouteMatch()
  const { pathname } = useLocation()
  const history = useHistory()
  return {
    get: () => {
      const fullPath = pathname.substring(url.length)
      return deep || fullPath === '' ? fullPath : '/' + fullPath.split('/')[1]
    },
    set: (path: string) =>
      replace ? history.replace(url + path) : history.push(url + path)
  }
}

export default useSubPath
