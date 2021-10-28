import { indexOf, removeEditorTab, WorkspaceTab } from './workspace-tabs'
import { InstructionsWorkspaceTab } from '../components/workspace/tab-panel/InstructionsTabPanel'
import { EditorWorkspaceTab } from '../components/workspace/tab-panel/EditorTabPanel'

test('removeEditorTab', () => {
  const tabToRemove = new EditorWorkspaceTab('foo.txt')
  const withoutTab: WorkspaceTab[] = [new InstructionsWorkspaceTab()]
  const withTab: WorkspaceTab[] = [tabToRemove, ...withoutTab]

  expect(removeEditorTab('foo.txt', withoutTab)).toStrictEqual(withoutTab)
  expect(removeEditorTab('foo.txt', withTab)).toStrictEqual(withoutTab)
})

test('indexOf', () => {
  const tab = new EditorWorkspaceTab('foo.txt')
  const withoutTab: WorkspaceTab[] = [new InstructionsWorkspaceTab()]
  const withTab: WorkspaceTab[] = [tab, ...withoutTab]

  expect(indexOf(withTab, tab)).toBe(0)
  expect(indexOf(withoutTab, tab)).toBe(-1)
})
