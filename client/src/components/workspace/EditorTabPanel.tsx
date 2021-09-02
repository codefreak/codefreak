import Editor, { Monaco } from '@monaco-editor/react'
import TabPanel, { LoadingTabPanelPlaceholder } from './TabPanel'
import useWorkspace from '../../hooks/useWorkspace'
import { useEffect, useState } from 'react'
import { extractRelativeFilePath, readFilePath } from '../../services/workspace'
import { debounce } from 'ts-debounce'
import { editor } from 'monaco-editor'
import { messageService } from '../../services/message'

type EditorTabPanelProps = {
  baseUrl: string
  file: string
}

const EditorTabPanel = ({ baseUrl, file }: EditorTabPanelProps) => {
  const { isWorkspaceAvailable, getFile, saveFile } = useWorkspace(baseUrl)
  const [isLoadingFile, setLoadingFile] = useState(false)
  const [fileContents, setFileContents] =
    useState<string | undefined>(undefined)

  useEffect(() => {
    // get initial file
    if (isWorkspaceAvailable && !isLoadingFile && fileContents === undefined) {
      getFile(file, true).then(value => {
        setFileContents(value)
        setLoadingFile(false)
      })
      setLoadingFile(true)
    }
  }, [isWorkspaceAvailable, getFile, isLoadingFile, fileContents, file])

  const filePath = readFilePath(baseUrl, file)

  const handleChange = debounce((contents?: string) => {
    // autosave
    if (contents) {
      const path = extractRelativeFilePath(filePath)
      saveFile(path, contents)
      setFileContents(contents)
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
          await saveFile(path, contents)
          messageService.success(`${path} saved`)
        }
      }
    })
  }

  const loading = !isWorkspaceAvailable || isLoadingFile

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
