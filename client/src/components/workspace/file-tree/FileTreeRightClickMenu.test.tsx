import { act } from '@testing-library/react'
import { render, waitForTime } from '../../../services/testing'
import FileTreeRightClickMenu from './FileTreeRightClickMenu'

describe('<FileTreeRightClickMenu />', () => {
  it('renders only add-file and add-directory menu entries when no right-click-item is given', async () => {
    let textContent = ''

    await act(async () => {
      const { container } = render(<FileTreeRightClickMenu />)

      // Wait for the menu-animation to finish rendering
      await waitForTime(3)

      textContent = container.textContent ?? ''
    })

    expect(textContent).toContain('Add file')
    expect(textContent).toContain('Add directory')
    expect(textContent).not.toContain('Delete')
    expect(textContent).not.toContain('Rename')
  })

  it('renders all four menu entries when a right-clicked-item is given', async () => {
    const rightClickedItem = {
      path: 'foo.txt',
      isFile: true
    }

    let textContent = ''

    await act(async () => {
      const { container } = render(
        <FileTreeRightClickMenu rightClickedItem={rightClickedItem} />
      )

      // Wait for the menu-animation to finish rendering
      await waitForTime(3)

      textContent = container.textContent ?? ''
    })

    expect(textContent).toContain('Add file')
    expect(textContent).toContain('Add directory')
    expect(textContent).toContain('Delete')
    expect(textContent).toContain('Rename')
  })
})
