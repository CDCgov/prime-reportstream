import { render, screen } from "@testing-library/react";
import Section from "./Section";

describe('<Section />', () => {
    const fakeSection = {
        title: "Mock title",
        type: "Mock type",
        summary: "Mock summary",
    }

    beforeEach(() => {
        render(<Section section={fakeSection} />)
    })

    test('Section renders properties', () => {
        const header = screen.getByTestId("heading")
        const summary = screen.getByTestId("paragraph")
        expect(header.innerHTML).toEqual(fakeSection.title)
        expect(summary.innerHTML).toEqual(fakeSection.summary)
    })

})