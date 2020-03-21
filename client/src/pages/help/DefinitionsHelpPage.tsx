import { PageHeaderWrapper } from '@ant-design/pro-layout'
import content from '@codefreak/docs/definitions.adoc'
import React from 'react'
import Asciidoc from 'react-asciidoc'

const DefinitionsHelpPage: React.FC = () => (
  <>
    <PageHeaderWrapper />
    <Asciidoc className="asciidoc">{content}</Asciidoc>
  </>
)

export default DefinitionsHelpPage
