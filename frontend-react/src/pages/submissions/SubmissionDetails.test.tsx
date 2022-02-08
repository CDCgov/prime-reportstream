import { render, screen } from "@testing-library/react";
import ActionDetailsResource from "../../resources/ActionDetailsResource";
import SubmissionDetails, { DestinationItem, DetailItem } from './SubmissionDetails'
import { BrowserRouter } from "react-router-dom";

/* 
    Using the included regex can end up pulling various elements where the
    value has the parsed timestamp. Use a function 
*/
const timeRegex: RegExp = /[0-9]+:[0-9]+ [a-zA-Z]M/

const mockData: ActionDetailsResource = ActionDetailsResource.dummy()
jest.mock('rest-hooks', () => ({
    useResource: () => { return mockData },
    /* Must return children when mocking, otherwise nothing inside renders */
    NetworkErrorBoundary:
        ({ children }: { children: JSX.Element[] }) => { return <>{children}</> }
}))

describe("SubmissionDetails", () => {
    beforeEach(() => {
        /*
            TODO: Custom renderers to handle Router, etc. when
            we need to use more complex render settings
        */
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

    test("renders data to sub-components", async () => {
        /* Report ID DetailItem */
        const idElement = await screen.findByText(mockData.id)

        /* DestinationItem contents*/
        const receiverOrgName = await screen.findByText(mockData.destinations[0].organization)
        const transmissionDate = await screen.findByText("7 Apr 1970")
        /* TODO: This has to take a Matcher function but no useful documentation exists. Following
            current documentation (i.e. using the lambda seen here) results in errors.
           See: https://testing-library.com/docs/react-testing-library/cheatsheet/#text-match-options
        */
        // const transmissionTime = screen.getByText((content, element) => {
        //     if (!content.startsWith("7 Apr 1970")) return element
        // })
        const recordsTransmitted = await screen.findByText(mockData.destinations[0].itemCount)

        /* 
            As above, so below. Add any new elements needing unit test
            verification to this array to be tested! 
        */
        const testElements = [
            idElement,
            receiverOrgName,
            transmissionDate,
            // transmissionTime,
            recordsTransmitted
        ]

        for (let i = 0; i < testElements.length; i++) {
            expect(testElements[i]).toBeInTheDocument();
        }
    })

    test("renders error when data is not present", () => {

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
        expect(screen.getByText(timeRegex)).toBeInTheDocument();
        expect(screen.getByText(/3/i)).toBeInTheDocument();
    })
})
