import { PageHeaderWrapper } from '@ant-design/pro-layout'
import content from '@codefreak/docs/modules/for-teachers/pages/ide.adoc'
import React from 'react'
import Asciidoc from 'react-asciidoc'

const IdeHelpPage: React.FC = () => (
  <>
    <PageHeaderWrapper />
    <Asciidoc className="asciidoc">{content}</Asciidoc>
  </>
)

export default IdeHelpPage
