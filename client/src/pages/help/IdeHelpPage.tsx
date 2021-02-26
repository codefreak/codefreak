import { PageHeaderWrapper } from '@ant-design/pro-layout'
import content from '@codefreak/docs/modules/for-teachers/pages/ide.adoc'

const IdeHelpPage: React.FC = () => (
  <>
    <PageHeaderWrapper />
    <div
      className="asciidoc"
      dangerouslySetInnerHTML={{
        __html: content
      }}
    />
  </>
)

export default IdeHelpPage
