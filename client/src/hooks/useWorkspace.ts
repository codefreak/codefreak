import { useEffect, useState } from 'react'
import {
  readFilePath,
  WRITE_FILE_FORM_KEY,
  writeFilePath
} from '../services/workspace'

const useWorkspace = (baseUrl: string) => {
  const [isWorkspaceAvailable, setWorkspaceAvailable] = useState(false)
  const [interval, setTimerInterval] = useState<NodeJS.Timeout | null>(null)

  useEffect(() => {
    if (interval === null) {
      const newInterval = setInterval(() => {
        if (baseUrl.length > 0 && !isWorkspaceAvailable) {
          fetch(baseUrl, {
            method: 'GET'
          })
            .catch(() => setWorkspaceAvailable(false))
            .then(result => {
              setWorkspaceAvailable(result !== undefined)

              if (isWorkspaceAvailable && interval !== null) {
                clearInterval(interval)
              }
            })
        }
      }, 1000)
      setTimerInterval(newInterval)
    }

    return () => {
      if (interval !== null) {
        clearInterval(interval)
        setTimerInterval(null)
      }
    }
  }, [interval, isWorkspaceAvailable, baseUrl])

  const getFile = async (path: string, create = false) => {
    const result = await fetch(readFilePath(baseUrl, path), {
      method: 'GET'
    })

    if (result.ok) {
      return result.text()
    }

    if (!create) {
      return Promise.reject()
    }

    await saveFile(path, '')

    const newResult = await fetch(readFilePath(baseUrl, path), {
      method: 'GET'
    })

    return newResult.text()
  }

  const saveFile = (path: string, contents: string) => {
    const formData = new FormData()
    const file = new File([contents], path)
    formData.append(WRITE_FILE_FORM_KEY, file, path)

    return fetch(writeFilePath(baseUrl), {
      method: 'POST',
      body: formData
    })
  }

  const saveFileAndReloadContents = async (path: string, contents: string) => {
    await saveFile(path, contents)
    return getFile(path)
  }

  return {
    isWorkspaceAvailable,
    baseUrl,
    getFile,
    saveFile,
    saveFileAndReloadContents
  }
}

export default useWorkspace
