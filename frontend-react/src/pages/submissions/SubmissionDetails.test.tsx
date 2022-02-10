import { MatcherFunction, screen } from "@testing-library/react";

import ActionDetailsResource from "../../resources/ActionDetailsResource";
import { ResponseType, TestResponse } from "../../resources/TestResponse";
import { renderWithRouter } from "../../utils/CustomRenderUtils";

import SubmissionDetails, {
    DestinationItem,
    DetailItem,
} from "./SubmissionDetails";

/*
    Using the included regex can end up pulling various elements where the
    value has the parsed timestamp. Use a function
*/
const timeRegex: RegExp = /[0-9]{1,2}:[0-9]{1,2} [A,P]M/;
const mockData: ActionDetailsResource = new TestResponse(
    ResponseType.ACTION_DETAIL
).data;
jest.mock("rest-hooks", () => ({
    useResource: () => {
        return mockData;
    },
    /* Must return children when mocking, otherwise nothing inside renders */
    NetworkErrorBoundary: ({ children }: { children: JSX.Element[] }) => {
        return <>{children}</>;
    },
}));

describe("SubmissionDetails", () => {
    beforeEach(() => {
        renderWithRouter(<SubmissionDetails />);
    });

    test("renders without error", async () => {
        const container = await screen.findByTestId("container");
        expect(container).toBeInTheDocument();
    });

    test("renders data to sub-components", async () => {
        /* Custom matcher for transitionTime */
        const findTimeWithoutDate: MatcherFunction = (
            content,
            element
        ): boolean => {
            if (!content.includes("7 Apr 1970") && timeRegex.test(content))
                return true;
            return false;
        };

        /* Report ID DetailItem */
        const idElement = await screen.findByText(mockData.id);

        /* DestinationItem contents*/
        const receiverOrgName = await screen.findByText(
            mockData.destinations[0].organization
        );
        const transmissionDate = await screen.findByText("7 Apr 1970");
        const transmissionTime = screen.getByText(findTimeWithoutDate);
        const recordsTransmitted = await screen.findByText(
            mockData.destinations[0].itemCount
        );

        /*
            As above, so below. Add any new elements needing unit test
            verification to this array to be tested!
        */
        const testElements = [
            idElement,
            receiverOrgName,
            transmissionDate,
            transmissionTime,
            recordsTransmitted,
        ];

        for (let i of testElements) {
            expect(i).toBeInTheDocument();
        }
    });
});

describe("DetailItem", () => {
    beforeEach(() => {
        renderWithRouter(
            <DetailItem item="Test Item" content="Test Content" />
        );
    });

    test("renders content", () => {
        expect(screen.getByText(/test item/i)).toBeInTheDocument();
        expect(screen.getByText(/test content/i)).toBeInTheDocument();
    });
});

describe("DestinationItem", () => {
    beforeEach(() => {
        renderWithRouter(
            <DestinationItem destinationObj={mockData.destinations[0]} />
        );
    });

    test("renders content", () => {
        expect(screen.getByText(/transmission date/i)).toBeInTheDocument();
        expect(screen.getByText(/transmission time/i)).toBeInTheDocument();
        expect(screen.getByText(/records/i)).toBeInTheDocument();
        /*
            These must change if we ever change the sending_at property of
            our test ActionDetailResource in TestResponse.ts
        */
        expect(screen.getByText(/7 Apr 1970/i)).toBeInTheDocument();
        expect(screen.getByText(timeRegex)).toBeInTheDocument();
        expect(screen.getByText(/3/i)).toBeInTheDocument();
    });
});
