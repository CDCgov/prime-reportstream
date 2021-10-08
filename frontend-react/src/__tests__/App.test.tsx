import { render, screen } from '@testing-library/react'
import renderer from 'react-test-renderer';
import App from '../App'

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