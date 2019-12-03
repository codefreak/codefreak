import { Route } from '@ant-design/pro-layout/lib/typings'
import { ROLES } from './components/Authorized'
import AdminPage from './pages/AdminPage'
import AssignmentListPage from './pages/assignment/AssignmentListPage'
import AssignmentPage from './pages/assignment/AssignmentPage'
import CreateAssignmentPage from './pages/assignment/CreateAssignmentPage'
import TaskPage from './pages/task/TaskPage'

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
          authority: ROLES.TEACHER,
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
      path: '/admin',
      name: 'Administration',
      icon: 'setting',
      authority: ROLES.ADMIN,
      component: AdminPage
    }
  ]
}
