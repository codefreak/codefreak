import { message } from 'antd'

export const messageService = {
  error: (msg: string) => message.error(msg),
  success: (msg: string) => message.success(msg)
}
