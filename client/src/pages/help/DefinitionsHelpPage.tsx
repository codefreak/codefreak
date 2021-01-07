import { PageHeaderWrapper } from '@ant-design/pro-layout'
import content from '@codefreak/docs/modules/for-teachers/pages/definitions.adoc'
import Asciidoc from 'react-asciidoc'

const DefinitionsHelpPage: React.FC = () => (
  <>
    <PageHeaderWrapper />
    <Asciidoc className="asciidoc">{content}</Asciidoc>
  </>
)

export default DefinitionsHelpPage
