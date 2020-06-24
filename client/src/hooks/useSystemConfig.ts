import {
  SystemConfig,
  useGetSystemConfigQuery
} from '../services/codefreak-api'

type SystemConfigField = keyof SystemConfig

const useSystemConfig = <T extends SystemConfigField>(
  field: T
): { data: SystemConfig[T] | undefined; loading: boolean } => {
  const { data, loading } = useGetSystemConfigQuery()

  return { data: data?.systemConfig?.[field], loading }
}

export default useSystemConfig
