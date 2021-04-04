import { Route } from '@ant-design/pro-layout/lib/typings'
import { AUTHORITIES } from './hooks/useHasAuthority'
import AssignmentListPage from './pages/assignment/AssignmentListPage'
import AssignmentPage from './pages/assignment/AssignmentPage'
import CreateTaskPage from './pages/task/CreateTaskPage'
import TaskPage from './pages/task/TaskPage'
import TaskPoolPage from './pages/task/TaskPoolPage'
import {
  ContainerOutlined,
  FileTextOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons'

export const routerConfig: Route = {
  path: '/',
  routes: [
    {
      path: '/assignments',
      name: 'Assignments',
      icon: <ContainerOutlined />,
      hideChildrenInMenu: true,
      component: AssignmentListPage,
      children: [
        {
          path: '/assignments/:id',
          component: AssignmentPage
        }
      ]
    },
    {
      path: '/tasks/pool',
      name: 'Task Pool',
      icon: <FileTextOutlined />,
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
      key: 'docs',
      name: 'Documentation',
      icon: <QuestionCircleOutlined />,
      isUrl: true,
      target: '_blank',
      path: process.env.CODEFREAK_DOCS_BASE_URL
    }
  ]
}
