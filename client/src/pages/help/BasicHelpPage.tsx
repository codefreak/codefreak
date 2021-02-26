import { PageHeaderWrapper } from '@ant-design/pro-layout'
import content from '@codefreak/docs/modules/ROOT/pages/basics.adoc'

const BasicHelpPage: React.FC<{ noHeader?: boolean }> = props => (
  <>
    {props.noHeader ? null : <PageHeaderWrapper />}
    <div
      className="asciidoc"
      dangerouslySetInnerHTML={{
        __html: content
      }}
    />
  </>
)

export default BasicHelpPage
