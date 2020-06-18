import { BASE_PATHS, Entity, getEntityPath } from './entity-path'

interface EntityWithTitle extends Entity {
  title: string
}

const forAssignment = (assignment: EntityWithTitle) => [
  { path: BASE_PATHS.Assignment, breadcrumbName: 'Assignments' },
  {
    path: getEntityPath(assignment),
    breadcrumbName: assignment.title
  }
]

interface Task extends EntityWithTitle {
  assignment?: EntityWithTitle | null
}

const forTask = (task: Task) => {
  const base = task.assignment
    ? forAssignment(task.assignment)
    : [{ path: BASE_PATHS.Task + '/pool', breadcrumbName: 'Task Pool' }]
  return [...base, { path: getEntityPath(task), breadcrumbName: task.title }]
}

export const createRoutes = {
  forAssignment,
  forTask
}
