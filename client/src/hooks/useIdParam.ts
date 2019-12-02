import { useParams } from 'react-router'

const useIdParam = () => {
  const { id } = useParams()
  if (id === undefined) {
    throw new Error("Path parameter 'id' not found")
  }
  return id
}

export default useIdParam
