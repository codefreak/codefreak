interface Entity {
  id: string
  title: string
}

const forAssignment = (assignment: Entity) => [
  { path: '/assignments', breadcrumbName: 'Assignments' },
  {
    path: '/assignments/' + assignment.id,
    breadcrumbName: assignment.title
  }
]

interface Task extends Entity {
  assignment: Entity
}

const forTask = (task: Task) => [
  ...forAssignment(task.assignment),
  { path: '/tasks/' + task.id, breadcrumbName: task.title }
]

export const createRoutes = {
  forAssignment,
  forTask
}
