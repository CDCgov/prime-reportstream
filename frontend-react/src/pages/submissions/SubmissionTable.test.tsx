import { screen, within } from "@testing-library/react";

import { renderWithFullAppContext } from "../../utils/CustomRenderUtils";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { mockAuthReturnValue } from "../../hooks/api/__mocks__/OktaAuth";

import SubmissionTable from "./SubmissionTable";

// const { addFeatureFlag, removeFeatureFlag } = _exportForTesting;

jest.mock("../../hooks/api/Deliveries/UseOrganizationSubmissions", () => ({
    useOrganizationSubmissions: (_: any, options: any) => {
        if (options.enabled) {
            return {
                data: [
                    { submissionId: 0 },
                    { submissionId: 1 },
                ] as OrganizationSubmission[],
            };
        }

        return { data: undefined };
    },
}));

describe("SubmissionTable", () => {
    test("renders a placeholder", async () => {
        mockAuthReturnValue({
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "testSender",
            },
            dispatch: () => {},
            initialized: true,
        });
        renderWithFullAppContext(<SubmissionTable />);

        const pagination = await screen.findByLabelText(
            /submissions pagination/i
        );
        expect(pagination).toBeInTheDocument();

        const filter = await screen.findByTestId("filter-container");
        expect(filter).toBeInTheDocument();

        const rowGroups = screen.getAllByRole("rowgroup");
        expect(rowGroups).toHaveLength(2);
        const tBody = rowGroups[1];
        const rows = within(tBody).getAllByRole("row");
        expect(rows).toHaveLength(2);
    });
});
