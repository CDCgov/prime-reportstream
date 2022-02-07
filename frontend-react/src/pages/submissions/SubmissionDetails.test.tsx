import { render, screen } from "@testing-library/react";
import ActionDetailsResource from "../../resources/ActionDetailsResource";
import SubmissionDetails, { DestinationItem, DetailItem } from './SubmissionDetails'
import { BrowserRouter } from "react-router-dom";
import { Destination } from "../../types/SubmissionDetailsTypes";

const mockData: ActionDetailsResource = ActionDetailsResource.dummy()
jest.mock('rest-hooks', () => ({
    useResource: () => { return mockData },
    NetworkErrorBoundary: () => { return (<div></div>) }
}))

describe("SubmissionDetails.txs", () => {
    beforeEach(() => {
        render(
            <BrowserRouter>
                <SubmissionDetails />
            </BrowserRouter>
        )
    })

    test("renders without error", async () => {
        const container = await screen.findByTestId("container");
        expect(container).toBeInTheDocument();
    })
})

describe("DetailItem", () => {
    beforeEach(() => {
        render(
            <BrowserRouter>
                <DetailItem
                    item="Test Item"
                    content="Test Content" />
            </BrowserRouter>
        )
    })

    test("renders content", () => {
        expect(screen.getByText(/test item/i)).toBeInTheDocument();
        expect(screen.getByText(/test content/i)).toBeInTheDocument();
    })
})

describe("DestinationItem", () => {
    beforeEach(() => {
        render(
            <BrowserRouter>
                <DestinationItem
                    destinationObj={mockData.destinations[0]}
                />
            </BrowserRouter>
        )
    })

    test("renders content", () => {
        expect(screen.getByText(/transmission date/i)).toBeInTheDocument();
        expect(screen.getByText(/transmission time/i)).toBeInTheDocument();
        expect(screen.getByText(/records/i)).toBeInTheDocument();
        /*
            These must change if we ever change the sending_at property of
            ActionDetailsResource.dummy() 
        */
        expect(screen.getByText(/7 Apr 1970/i)).toBeInTheDocument();
        expect(screen.getByText(/11:26/i)).toBeInTheDocument();
        expect(screen.getByText(/3/i)).toBeInTheDocument();
    })
})