import { render, screen } from "@testing-library/react"
import content from '../../content/content.json'
import Hero from "./Hero"

describe('<Hero />', () => {

    beforeEach(() => {
        render(<Hero />)
    })

    test('Title renders on Hero', () => {
        const text = screen.getByText(content.title)
        expect(text).not.toBeNull
    })

    test('Summary renders on Hero', () => {
        const text = screen.getByText(content.summary)
        expect(text).not.toBeNull
    })

})