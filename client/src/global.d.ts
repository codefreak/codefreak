declare global {
  /**
   * Object that is exposed by the Spring backend via window.__CODEFREAK_ERROR
   * for server-side errors
   */
  interface CodefreakError {
    timestamp: string
    status: number
    message: string
    error: string
    trace?: string
    path: string
  }

  interface Window {
    __CODEFREAK_ERROR: CodefreakError
  }
}

export {}
