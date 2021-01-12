import { Route } from '@ant-design/pro-layout/lib/typings'
import { AUTHORITIES } from './hooks/useHasAuthority'
import AdminPage from './pages/AdminPage'
import AssignmentListPage from './pages/assignment/AssignmentListPage'
import AssignmentPage from './pages/assignment/AssignmentPage'
import CreateAssignmentPage from './pages/assignment/CreateAssignmentPage'
import {
  BasicHelpPage,
  DefinitionsHelpPage,
  HelpPage,
  IdeHelpPage
} from './pages/help'
import CreateTaskPage from './pages/task/CreateTaskPage'
import TaskPage from './pages/task/TaskPage'
import TaskPoolPage from './pages/task/TaskPoolPage'
import {
  ContainerOutlined,
  FileTextOutlined,
  QuestionCircleOutlined,
  SettingOutlined
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
      name: 'Help',
      icon: <QuestionCircleOutlined />,
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
        },
        {
          path: '/help/ide',
          name: 'Online IDE',
          component: IdeHelpPage,
          authority: AUTHORITIES.ROLE_TEACHER
        }
      ]
    },
    {
      path: '/admin',
      name: 'Administration',
      icon: <SettingOutlined />,
      authority: AUTHORITIES.ROLE_ADMIN,
      component: AdminPage
    }
  ]
}
