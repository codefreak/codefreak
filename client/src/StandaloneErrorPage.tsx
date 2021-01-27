import Centered from './components/Centered'
import Logo from './components/Logo'
import { Button } from 'antd'
import { HomeFilled } from '@ant-design/icons'

import './StandaloneErrorPage.less'

interface StandaloneErrorPageProps {
  error: CodefreakError
}

const StandaloneErrorPage: React.FC<StandaloneErrorPageProps> = ({ error }) => {
  return (
    <Centered>
      <div className="error-root">
        <div>
          <a href="/">
            <Logo className="codefreak-logo" width={200} />
          </a>
        </div>
        <h1>
          Error {error.status} – {error.error}
        </h1>
        <p className="error-details">{error.message}</p>
        <Button href="/" type="primary" icon={<HomeFilled />} size="large">
          Return to Home
        </Button>
      </div>
    </Centered>
  )
}

export default StandaloneErrorPage
