import { message } from 'antd'

export const messageService = {
  error: (msg: string) => {
    message.error(msg, 5)
  },
  success: (msg: string) => {
    message.success(msg)
  }
}
