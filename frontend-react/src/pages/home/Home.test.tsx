import { render, screen } from "@testing-library/react";
import Home from "./Home";
import content from '../../content/content.json'

jest.mock("@cdc/map", () => () => {
    return <div>Map</div>;
});

describe('Home rendering', () => {

    beforeEach(() => {
        render(<Home />)
    })

    test('Container renders', () => {
        expect(screen.getByTestId('container')).toBeInTheDocument();
    })

    test('Renders correct number of elements', async () => {
        content.sections.map(async section => {
            expect(await screen.findAllByTestId('feature')).toHaveLength(section.features?.length || 0)
        })
        expect(await screen.findAllByTestId('section')).toHaveLength(content.sections.length)
        expect(await screen.findAllByTestId('free-secure')).toHaveLength(content.freeSecure.length)
    })

})