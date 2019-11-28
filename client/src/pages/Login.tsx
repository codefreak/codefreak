import { useApolloClient } from '@apollo/react-hooks'
import { Button, Card, Form, Icon, Input } from 'antd'
import { FormComponentProps } from 'antd/lib/form'
import React from 'react'
import {
  LoginDocument,
  LoginMutationResult,
  User
} from '../services/codefreak-api'

interface Credentials {
  username: string
  password: string
}

interface LoginProps extends FormComponentProps<Credentials> {
  setAuthenticatedUser: (user: User) => void
}

const Login: React.FC<LoginProps> = props => {
  const { getFieldDecorator } = props.form

  const apolloClient = useApolloClient()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    props.form.validateFields((err, values) => {
      if (err) {
        return
      }
      apolloClient
        .mutate({
          mutation: LoginDocument,
          variables: values
        })
        .then(res => {
          const authentication = (res as LoginMutationResult).data!.login
          props.setAuthenticatedUser(authentication.user)
        })
    })
  }
  return (
    <div
      style={{
        height: '100%',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'grey'
      }}
    >
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
            <Button type="primary" htmlType="submit" style={{ width: '100%' }}>
              Log in
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Form.create<LoginProps>({ name: 'login' })(Login)
