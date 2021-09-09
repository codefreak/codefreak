import { render } from '../../services/testing'
import WorkspacePage from './WorkspacePage'
import { FileContextType } from '../../services/codefreak-api'

describe('<WorkspacePage />', () => {
  it('renders <WorkspaceTabsWrapper /> two times', () => {
    const { container } = render(
      <WorkspacePage id="" type={FileContextType.Answer} />
    )

    expect(
      container.getElementsByClassName('workspace-tabs-wrapper')
    ).toHaveLength(2)
  })
})
