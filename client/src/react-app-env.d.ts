/// <reference types="react-scripts" />

declare module 'react-asciidoc'

declare module '*.adoc' {
  const content: string
  export default content
}
