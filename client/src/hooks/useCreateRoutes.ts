import { BASE_PATHS, Entity, getEntityPath } from '../services/entity-path'
import { useHideNavigation } from './useHideNavigation'

interface EntityWithTitle extends Entity {
  title: string
}

const forAssignment = (noBase: boolean) => (assignment: EntityWithTitle) => {
  const base = noBase
    ? []
    : [{ path: BASE_PATHS.Assignment, breadcrumbName: 'Assignments' }]
  return [
    ...base,
    {
      path: getEntityPath(assignment),
      breadcrumbName: assignment.title
    }
  ]
}

interface Task extends EntityWithTitle {
  assignment?: EntityWithTitle | null
}

const forTask = (noBase: boolean) => (task: Task) => {
  const base = task.assignment
    ? forAssignment(noBase)(task.assignment)
    : [{ path: BASE_PATHS.Task + '/pool', breadcrumbName: 'Task Pool' }]
  return [...base, { path: getEntityPath(task), breadcrumbName: task.title }]
}

export const useCreateRoutes = () => {
  const noBase = useHideNavigation()
  return {
    forAssignment: forAssignment(noBase),
    forTask: forTask(noBase)
  }
}
