/// <reference types="react-scripts" />

declare module 'react-asciidoc'

declare module 'json-to-pretty-yaml'

declare module '*.adoc' {
  const content: string
  export default content
}
