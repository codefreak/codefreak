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

  /**
   * Make TS aware of our custom environment variables defined
   * via Webpack's DefinePlugin (see craco.config.js)
   */
  namespace NodeJS {
    interface ProcessEnv {
      CODEFREAK_DOCS_BASE_URL: string
      BUILD_YEAR: string
      BUILD_VERSION: string
      BUILD_HASH: string
    }
  }
}

export {}
