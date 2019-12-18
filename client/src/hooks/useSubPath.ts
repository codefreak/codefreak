import { useHistory, useLocation, useRouteMatch } from 'react-router'

const useSubPath = (replace: boolean = true) => {
  const { url } = useRouteMatch()
  const { pathname } = useLocation()
  const history = useHistory()
  return {
    get: () => pathname.substring(url.length),
    set: (path: string) =>
      replace ? history.replace(url + path) : history.push(url + path)
  }
}

export default useSubPath
