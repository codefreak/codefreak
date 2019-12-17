import { useParams } from 'react-router'
import { unshorten } from '../services/short-id'

const useIdParam = () => {
  const { id } = useParams()
  if (id === undefined) {
    throw new Error("Path parameter 'id' not found")
  }
  return unshorten(id)
}

export default useIdParam
