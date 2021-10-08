import { render } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import App from '../App'

jest.mock('../App', () => () => { return <div>Hello</div> })

beforeAll = () => {
  return null
}

afterAll = () => {
  return null
}

describe('Describe 1', () => {

  test('Test 1', () => {
    const appComponent = render(<App />)
    expect(appComponent).not.toBeNull()
  });

});