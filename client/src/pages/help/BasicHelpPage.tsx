import { PageHeaderWrapper } from '@ant-design/pro-layout'
import content from '@codefreak/docs/basics.adoc'
import React from 'react'
import Asciidoc from 'react-asciidoc'

const BasicHelpPage: React.FC<{ noHeader?: boolean }> = props => (
  <>
    {props.noHeader ? null : <PageHeaderWrapper />}
    <Asciidoc className="asciidoc">{content}</Asciidoc>
  </>
)

export default BasicHelpPage
