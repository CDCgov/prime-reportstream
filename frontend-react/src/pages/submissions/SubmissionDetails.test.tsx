import { render, screen } from "@testing-library/react";
import TestRenderer from 'react-test-renderer';
import ActionDetailsResource from "../../resources/ActionDetailsResource";
import SubmissionDetails, { DestinationItem, DetailItem } from './SubmissionDetails'
import { BrowserRouter } from "react-router-dom";
import { Destination } from "../../types/SubmissionDetailsTypes";
import { Component } from "spinners-react";

const mockData: ActionDetailsResource = ActionDetailsResource.dummy()
jest.mock('rest-hooks', () => ({
    useResource: () => { return mockData },
    /* Must return children when mocking, otherwise nothing inside renders */
    NetworkErrorBoundary:
        ({ children }: { children: JSX.Element[] }) => { return <>{children}</> }
}))

describe("SubmissionDetails w/ data", () => {
    beforeEach(() => {
        /*
            TODO: Custom renderers to handle Router, etc. when
            we need to use more complex render settings
        */
        let component = render(
            <BrowserRouter>
                <SubmissionDetails />
            </BrowserRouter>
        )
    })

    test("renders without error", async () => {
        const container = await screen.findByTestId("container");
        expect(container).toBeInTheDocument();
    })

    test("renders data to sub-components", async () => {
        const idElement = await screen.findByText(mockData.id)
        const receiverOrgName = await screen.findByText(mockData.destinations[0].organization)

        /* 
            As above, so below. Add any new elements needing unit test
            verification to this array to be tested! 
        */
        const testElements = [
            idElement,
            receiverOrgName
        ]

        for (let i = 0; i < testElements.length; i++) {
            expect(testElements[i]).toBeInTheDocument();
        }
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