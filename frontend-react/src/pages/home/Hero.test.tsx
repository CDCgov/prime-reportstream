import { render, screen } from "@testing-library/react"
import content from '../../content/content.json'
import Hero from "./Hero"

describe('<Hero />', () => {

    beforeEach(() => {
        render(<Hero />)
    })

    test('Title and Summary render on Hero', () => {
        const title = screen.getByText(content.title)
        const summary = screen.getByText(content.summary)
        expect(title).not.toBeNull
        expect(summary).not.toBeNull
    })

})