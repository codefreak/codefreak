import { render, unmountComponentAtNode } from 'react-dom'
import { act } from 'react-dom/test-utils'
import { GetAssignmentWithSubmissionsQueryResult } from '../services/codefreak-api'

import SubmissionsTable from './SubmissionsTable'

type Assignment = NonNullable<
  GetAssignmentWithSubmissionsQueryResult['data']
>['assignment']
type Submission = Assignment['submissions'][number]
type User = Submission['user']

let container: Element

beforeEach(() => {
  container = document.createElement('div')
  document.body.appendChild(container)

  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation(query => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: jest.fn(), // deprecated, but antd uses it
      removeListener: jest.fn(), // deprecated, but antd uses it
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      dispatchEvent: jest.fn()
    }))
  })
})

afterEach(() => {
  unmountComponentAtNode(container)
  container?.remove()
})

it('renders user data', () => {
  const dummyUser: User = {
    id: '',
    username: 'john-doe',
    firstName: 'John',
    lastName: 'Doe'
  }
  const dummySubmission: Submission = {
    id: 'id',
    user: dummyUser,
    answers: []
  }
  const dummyAssignment: Assignment = {
    title: 'Dummy assignment',
    id: 'dummy',
    submissionsDownloadUrl: '',
    submissions: [dummySubmission],
    tasks: []
  }

  act(() => {
    render(<SubmissionsTable assignment={dummyAssignment} />, container)
  })

  expect(container.textContent).toContain(dummyUser.firstName)
  expect(container.textContent).toContain(dummyUser.lastName)
  expect(container.textContent).toContain(dummyUser.username)
})
