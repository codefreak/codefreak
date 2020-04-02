import { Button, Card, Form, Icon, Input } from 'antd'
import { FormComponentProps } from 'antd/lib/form'
import React, { useEffect } from 'react'
import Centered from '../components/Centered'
import Logo from '../components/Logo'
import { AuthenticatedUser } from '../hooks/useAuthenticatedUser'
import { useLoginMutation } from '../services/codefreak-api'

interface Credentials {
  username: string
  password: string
}

interface LoginProps extends FormComponentProps<Credentials> {
  onSuccessfulLogin: (user: AuthenticatedUser) => void
}

const LoginPage: React.FC<LoginProps> = props => {
  const { getFieldDecorator } = props.form
  const { onSuccessfulLogin } = props

  const [login, { data, loading }] = useLoginMutation()

  useEffect(() => {
    if (data !== undefined) {
      onSuccessfulLogin(data.login.user)
    }
  }, [onSuccessfulLogin, data])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    props.form.validateFields((err, values) => {
      if (!err) {
        login({ variables: values })
      }
    })
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
        <Form onSubmit={handleSubmit}>
          <Form.Item>
            {getFieldDecorator('username', {
              rules: [
                { required: true, message: 'Please input your username!' }
              ]
            })(
              <Input
                prefix={
                  <Icon type="user" style={{ color: 'rgba(0,0,0,.25)' }} />
                }
                autoComplete="username"
                autoFocus
                placeholder="Username / Mail Address"
              />
            )}
          </Form.Item>
          <Form.Item>
            {getFieldDecorator('password', {
              rules: [
                { required: true, message: 'Please input your password!' }
              ]
            })(
              <Input
                prefix={
                  <Icon type="lock" style={{ color: 'rgba(0,0,0,.25)' }} />
                }
                autoComplete="password"
                type="password"
                placeholder="Password"
              />
            )}
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

export default Form.create<LoginProps>({ name: 'login' })(LoginPage)
