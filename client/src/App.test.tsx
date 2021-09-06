import { render } from '@testing-library/react'
import App from './App'
import { MockedProvider } from '@apollo/client/testing'

it('renders without crashing', () => {
  render(
    <MockedProvider>
      <App />
    </MockedProvider>
  )
})
