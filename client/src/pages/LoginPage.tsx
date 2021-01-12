import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Card, Form, Input } from 'antd'
import { useEffect } from 'react'
import Centered from '../components/Centered'
import Logo from '../components/Logo'
import { AuthenticatedUser } from '../hooks/useAuthenticatedUser'
import { useLoginMutation } from '../services/codefreak-api'
import { FormProps } from 'antd/es/form'

interface Credentials {
  username: string
  password: string
}

interface LoginProps extends FormProps<Credentials> {
  onSuccessfulLogin: (user: AuthenticatedUser) => void
}

const LoginPage: React.FC<LoginProps> = props => {
  const { onSuccessfulLogin } = props

  const [login, { data, loading }] = useLoginMutation()

  useEffect(() => {
    if (data) {
      onSuccessfulLogin(data.login.user)
    }
  }, [onSuccessfulLogin, data])

  const handleSubmit: FormProps['onFinish'] = async values => {
    await login({ variables: values })
  }

  return (
    <Centered className="background-carbon">
      <Card
        title={
          <h1
            style={{
              marginBottom: -16,
              textAlign: 'center'
            }}
          >
            <Logo height={64} />
          </h1>
        }
        style={{ width: '100%', maxWidth: 300, margin: 16 }}
        headStyle={{ borderBottom: 'none' }}
      >
        <Form onFinish={handleSubmit} name="login">
          <Form.Item
            name="username"
            rules={[
              {
                required: true,
                message: 'Please input your username!'
              }
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
              autoComplete="username"
              autoFocus
              placeholder="Username / Mail Address"
            />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[
              {
                required: true,
                message: 'Please input your password!'
              }
            ]}
          >
            <Input
              prefix={<LockOutlined style={{ color: 'rgba(0,0,0,.25)' }} />}
              autoComplete="password"
              type="password"
              placeholder="Password"
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              style={{ width: '100%' }}
              loading={loading}
            >
              Sign in
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </Centered>
  )
}

export default LoginPage
