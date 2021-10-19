import { render } from '@testing-library/react'
import { GetAssignmentWithSubmissionsQueryResult } from '../services/codefreak-api'

import SubmissionsTable from './SubmissionsTable'

type Assignment = NonNullable<
  GetAssignmentWithSubmissionsQueryResult['data']
>['assignment']
type Submission = Assignment['submissions'][number]
type User = Submission['user']

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

  const { container } = render(
    <SubmissionsTable assignment={dummyAssignment} />
  )

  expect(container.textContent).toContain(dummyUser.firstName)
  expect(container.textContent).toContain(dummyUser.lastName)
  expect(container.textContent).toContain(dummyUser.username)
})
