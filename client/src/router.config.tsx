import { Route } from '@ant-design/pro-layout/lib/typings'
import { AUTHORITIES } from './hooks/useHasAuthority'
import AdminPage from './pages/AdminPage'
import AssignmentListPage from './pages/assignment/AssignmentListPage'
import AssignmentPage from './pages/assignment/AssignmentPage'
import CreateAssignmentPage from './pages/assignment/CreateAssignmentPage'
import TaskPage from './pages/task/TaskPage'
import TaskPoolPage from './pages/task/TaskPoolPage'

export const routerConfig: Route = {
  path: '/',
  routes: [
    {
      path: '/assignments',
      name: 'Assignments',
      icon: 'container',
      hideChildrenInMenu: true,
      component: AssignmentListPage,
      children: [
        {
          path: '/assignments/create',
          name: 'Create Assignment',
          authority: AUTHORITIES.ROLE_TEACHER,
          component: CreateAssignmentPage
        },
        {
          path: '/assignments/:id',
          component: AssignmentPage
        }
      ]
    },
    {
      path: '/tasks/:id',
      component: TaskPage
    },
    {
      path: '/tasks',
      name: 'Task Pool',
      icon: 'file-text',
      component: TaskPoolPage,
      authority: AUTHORITIES.ROLE_TEACHER
    },
    {
      path: '/admin',
      name: 'Administration',
      icon: 'setting',
      authority: AUTHORITIES.ROLE_ADMIN,
      component: AdminPage
    }
  ]
}
