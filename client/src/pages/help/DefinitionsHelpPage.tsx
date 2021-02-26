import { PageHeaderWrapper } from '@ant-design/pro-layout'
import content from '@codefreak/docs/modules/for-teachers/pages/definitions.adoc'

const DefinitionsHelpPage: React.FC = () => (
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

export default DefinitionsHelpPage
