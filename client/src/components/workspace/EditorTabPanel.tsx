import Editor, { Monaco } from '@monaco-editor/react'
import TabPanel, { LoadingTabPanelPlaceholder } from './TabPanel'
import useWorkspace from '../../hooks/workspace/useWorkspace'
import { extractRelativeFilePath, readFilePath } from '../../services/workspace'
import { debounce } from 'ts-debounce'
import { editor } from 'monaco-editor'
import { messageService } from '../../services/message'
import useGetWorkspaceFileQuery from '../../hooks/workspace/useGetWorkspaceFileQuery'
import useSaveWorkspaceFileMutation from '../../hooks/workspace/useSaveWorkspaceFileMutation'
import { useEffect, useState } from 'react'

type EditorTabPanelProps = {
  file: string
}

const EditorTabPanel = ({ file }: EditorTabPanelProps) => {
  const { isAvailable, baseUrl } = useWorkspace()
  const { data, isLoading } = useGetWorkspaceFileQuery(file)
  const { mutate: saveFile } = useSaveWorkspaceFileMutation()

  const [fileContents, setFileContents] =
    useState<string | undefined>(undefined)

  useEffect(() => {
    if (data && fileContents === undefined) {
      setFileContents(data)
    }
  }, [data, fileContents])

  const filePath = readFilePath(baseUrl, file)

  const handleChange = debounce(async (contents?: string) => {
    // autosave
    if (contents !== undefined) {
      const path = extractRelativeFilePath(filePath)
      saveFile({ path, contents })
    }
  }, 250)

  const handleEditorDidMount = (
    mountedEditor: editor.IStandaloneCodeEditor,
    mountedMonaco: Monaco
  ) => {
    // cmd + s
    mountedEditor.addAction({
      id: 'save',
      label: 'Save',
      keybindings: [mountedMonaco.KeyMod.CtrlCmd | mountedMonaco.KeyCode.KEY_S],
      run: async ed => {
        const model = ed.getModel()

        if (model) {
          const path = extractRelativeFilePath(model.uri.path)
          const contents = model.getValue()
          saveFile({ path, contents })
          messageService.success(`${path} saved`)
        }
      }
    })
  }

  const loading = !isAvailable || isLoading

  return (
    <TabPanel loading={loading}>
      <Editor
        theme="light"
        value={fileContents}
        path={filePath}
        loading={<LoadingTabPanelPlaceholder />}
        onChange={handleChange}
        onMount={handleEditorDidMount}
      />
    </TabPanel>
  )
}

export default EditorTabPanel
