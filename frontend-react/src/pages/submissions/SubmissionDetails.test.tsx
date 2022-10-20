import { MatcherFunction, screen } from "@testing-library/react";

import ActionDetailsResource from "../../resources/ActionDetailsResource";
import { ResponseType, TestResponse } from "../../resources/TestResponse";
import { renderWithRouter } from "../../utils/CustomRenderUtils";
import { DetailItem } from "../../components/DetailItem/DetailItem";

import SubmissionDetails, { DestinationItem } from "./SubmissionDetails";

/*
    Using the included regex can end up pulling various elements where the
    value has the parsed timestamp. Use a function
*/
const dateRegex = /\d{1,2} [a-z,A-Z]{3} \d{4}/;
const timeRegex: RegExp = /\d{1,2}:\d{2}/;

/*
    We can only mock one behavior for useResource currently. This is a major
    limitation for us that doesn't allow us to test negative cases.
*/
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

    test("renders crumb nav to Submissions list", () => {
        const submissionCrumb = screen.getByRole("link");
        expect(submissionCrumb).toBeInTheDocument();
        expect(submissionCrumb).toHaveTextContent("Submissions");
    });

    test("renders without error", async () => {
        const container = await screen.findByTestId("container");
        expect(container).toBeInTheDocument();
    });

    test("renders data to sub-components", async () => {
        /* Custom matcher for transitionTime */
        const findTimeWithoutDate: MatcherFunction = (content): boolean => {
            return !content.includes("7 Apr 1970") && timeRegex.test(content);
        };

        /* Report ID DetailItem */
        const idElement = await screen.findByText(mockData.id);

        /* DestinationItem contents*/
        const receiverOrgNameAndService = await screen.findByText(
            `${mockData.destinations[0].organization}`
        );
        const dataStream = await screen.findByText(
            mockData.destinations[0].service.toUpperCase()
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
            receiverOrgNameAndService,
            dataStream,
            transmissionDate,
            transmissionTime,
            recordsTransmitted,
        ];

        for (let i of testElements) {
            expect(i).toBeInTheDocument();
        }
    });

    test("Filename conditionally shows in title", () => {
        /*
            TODO: How can we use the object and not static strings to
            check for substrings like this??
        */
        const title = screen.getByText(/SubmissionDetails Unit Test/);
        expect(title).toBeInTheDocument();
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
        expect(screen.getByText(/primary/i)).toBeInTheDocument();
        /*
            These must change if we ever change the sending_at property of
            our test ActionDetailResource in TestResponse.ts
        */
        expect(screen.getByText(dateRegex)).toBeInTheDocument();
        expect(screen.getByText(timeRegex)).toBeInTheDocument();
        expect(screen.getByText(/3/i)).toBeInTheDocument();
    });
});
