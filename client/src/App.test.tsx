import ReactDOM from 'react-dom'
import App from './App'
import { MockedProvider } from '@apollo/client/testing'

it('renders without crashing', () => {
  const div = document.createElement('div')
  ReactDOM.render(
    <MockedProvider>
      <App />
    </MockedProvider>,
    div
  )
  ReactDOM.unmountComponentAtNode(div)
})
