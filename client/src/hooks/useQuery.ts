import { useLocation } from 'react-router-dom'

export const useQuery = () => {
  return new URLSearchParams(useLocation().search)
}

export const useQueryParam = (param: string) => {
  return useQuery().get(param) || undefined
}
