import { render } from '../../services/testing'
import EmptyTabPanel from './EmptyTabPanel'

describe('<EmptyTabPanel />', () => {
  it('has no content', () => {
    const { container } = render(<EmptyTabPanel />)

    expect(container.textContent).toBe('')
    expect(
      container.getElementsByClassName('workspace-tab-panel')
    ).toHaveLength(1)
  })
})
