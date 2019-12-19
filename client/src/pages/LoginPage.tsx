import { Button, Card, Form, Icon, Input } from 'antd'
import { FormComponentProps } from 'antd/lib/form'
import React, { useEffect } from 'react'
import Centered from '../components/Centered'
import { AuthenticatedUser } from '../hooks/useAuthenticatedUser'
import { useLoginMutation } from '../services/codefreak-api'

const backgroundStyle = `
background:
linear-gradient(27deg, #151515 5px, transparent 5px) 0 5px,
linear-gradient(207deg, #151515 5px, transparent 5px) 10px 0px,
linear-gradient(27deg, #222 5px, transparent 5px) 0px 10px,
linear-gradient(207deg, #222 5px, transparent 5px) 10px 5px,
linear-gradient(90deg, #1b1b1b 10px, transparent 10px),
linear-gradient(#1d1d1d 25%, #1a1a1a 25%, #1a1a1a 50%, transparent 50%, transparent 75%, #242424 75%, #242424);
background-color: #131313;
background-size: 20px 20px;
`

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
      <Card title="Login" style={{ width: '100%', maxWidth: 300, margin: 16 }}>
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
                placeholder="Username"
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
