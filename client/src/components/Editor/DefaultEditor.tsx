import { editor, editor as monaco, Uri } from 'monaco-editor'
import React, { useEffect, useRef } from 'react'
import MonacoEditor, { EditorDidMount } from 'react-monaco-editor'

// default options for read-only mode
const ReadOnlyOptions: editor.IStandaloneEditorConstructionOptions = {
  readOnly: true,
  minimap: {
    enabled: false
  },
  scrollbar: {
    horizontal: 'auto',
    vertical: 'auto'
  },
  scrollBeyondLastLine: false,
  contextmenu: false
}

export interface EditorProps {
  monacoOptions?: editor.IStandaloneEditorConstructionOptions
  readOnly?: boolean
  currentLine?: number
  value: string
  path: string
  maxNumLines?: number
  decorations?: editor.IModelDeltaDecoration[]
  editorDidMount?: EditorDidMount
}

export const DefaultEditor: React.FC<EditorProps> = props => {
  const { value, path, editorDidMount, decorations } = props
  const monacoOptions: editor.IStandaloneEditorConstructionOptions = {
    ...props.monacoOptions
  }

  if (props.readOnly) {
    Object.assign(monacoOptions, ReadOnlyOptions)
  }

  const editorRef = useRef<editor.IStandaloneCodeEditor>()
  const fileUri = Uri.file(path)
  const model =
    monaco.getModel(fileUri) || monaco.createModel(value, undefined, fileUri)

  const onMount: EditorDidMount = (editorInstance, monacoApi) => {
    editorRef.current = editorInstance
    if (editorDidMount) {
      editorDidMount(editorInstance, monacoApi)
    }
  }

  useEffect(() => {
    if (editorRef.current) {
      editorRef.current.setModel(model)
    }
  }, [model])

  useEffect(() => {
    if (editorRef.current && decorations) {
      editorRef.current.deltaDecorations([], decorations)
    }
  }, [decorations])

  useEffect(() => {
    if (editorRef.current && props.currentLine) {
      editorRef.current.revealLineInCenter(props.currentLine)
    }
  }, [props.currentLine])

  // 19px is the default line height
  const height = props.maxNumLines ? 19 * props.maxNumLines : '100%'

  return (
    <div className="code-editor-wrapper" style={{ height }}>
      <MonacoEditor options={monacoOptions} editorDidMount={onMount} />
    </div>
  )
}

DefaultEditor.defaultProps = {
  readOnly: true
}

export default DefaultEditor
