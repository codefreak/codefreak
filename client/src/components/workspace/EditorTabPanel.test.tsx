import { render } from '@testing-library/react'
import { MockedProvider } from '@apollo/client/testing'
import EditorTabPanel from './EditorTabPanel'

describe('<EditorTabPanel />', () => {
  it('renders a <TabPanel />', () => {
    const { container } = render(
      <MockedProvider>
        <EditorTabPanel baseUrl="" file="file" />
      </MockedProvider>
    )

    expect(
      container.getElementsByClassName('workspace-tab-panel')
    ).toHaveLength(1)
  })
})
