import { Route } from '@ant-design/pro-layout/lib/typings'
import { AUTHORITIES } from './hooks/useHasAuthority'
import AdminPage from './pages/AdminPage'
import AssignmentListPage from './pages/assignment/AssignmentListPage'
import AssignmentPage from './pages/assignment/AssignmentPage'
import CreateAssignmentPage from './pages/assignment/CreateAssignmentPage'
import { BasicHelpPage, DefinitionsHelpPage, HelpPage } from './pages/help'
import CreateTaskPage from './pages/task/CreateTaskPage'
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
      path: '/tasks/pool',
      name: 'Task Pool',
      icon: 'file-text',
      component: TaskPoolPage,
      authority: AUTHORITIES.ROLE_TEACHER,
      hideChildrenInMenu: true,
      children: [
        {
          path: '/tasks/pool/create',
          name: 'Create Task',
          authority: AUTHORITIES.ROLE_TEACHER,
          component: CreateTaskPage
        }
      ]
    },
    {
      path: '/tasks/:id',
      component: TaskPage
    },
    {
      name: 'Help',
      icon: 'question-circle',
      path: '/help',
      hideChildrenInMenu: true,
      component: HelpPage,
      children: [
        {
          path: '/help/basics',
          name: 'Basics',
          component: BasicHelpPage,
          authority: AUTHORITIES.ROLE_TEACHER
        },
        {
          path: '/help/definitions',
          name: 'Definition Files',
          component: DefinitionsHelpPage,
          authority: AUTHORITIES.ROLE_TEACHER
        }
      ]
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
