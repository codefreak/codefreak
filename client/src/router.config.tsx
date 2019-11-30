import { Route } from '@ant-design/pro-layout/lib/typings'
import AdminPage from './pages/AdminPage'
import AssignmentListPage from './pages/assignment/AssignmentListPage'
import AssignmentPage from './pages/assignment/AssignmentPage'
import CreateAssignmentPage from './pages/assignment/CreateAssignmentPage'

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
          component: CreateAssignmentPage
        },
        {
          path: '/assignments/:id',
          component: AssignmentPage
        }
      ]
    },
    {
      path: '/admin',
      name: 'Administration',
      icon: 'setting',
      component: AdminPage
    }
  ]
}
