import { render } from '@testing-library/react'
import WorkspacePage from './WorkspacePage'
import { FileContextType } from '../../services/codefreak-api'
import { MockedProvider } from '@apollo/client/testing'

describe('<WorkspacePage />', () => {
  it('renders <WorkspaceTabsWrapper /> two times', () => {
    const { container } = render(
      <MockedProvider>
        <WorkspacePage id="" type={FileContextType.Task} />
      </MockedProvider>
    )

    expect(
      container.getElementsByClassName('workspace-tabs-wrapper')
    ).toHaveLength(2)
  })
})
