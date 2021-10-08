import { render, screen } from '@testing-library/react'

beforeAll = () => {
  return null
}

afterAll = () => {
  return null
}

describe('Describe 1', () => {

  test('Test 1', () => {
    const sum = (a: number, b: number) => { return a + b }
    expect(sum(4, 5)).toBe(9)
  });

});