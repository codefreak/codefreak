import useAuthenticatedUser from './useAuthenticatedUser'

export const AUTHORITIES = {
  ROLE_ADMIN: 'ROLE_ADMIN',
  ROLE_TEACHER: 'ROLE_TEACHER',
  ROLE_STUDENT: 'ROLE_STUDENT'
}

export type Authority = keyof typeof AUTHORITIES

const useHasAuthority = (authority: Authority) =>
  useAuthenticatedUser().authorities.includes(authority)

export default useHasAuthority
