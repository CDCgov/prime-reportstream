import { Fixture } from "@rest-hooks/test";
import { screen, within } from "@testing-library/react";

import SubmissionsResource from "../../resources/SubmissionsResource";
import { renderApp } from "../../utils/CustomRenderUtils";
import { mockSessionContext } from "../../contexts/__mocks__/SessionContext";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { Organizations } from "../../hooks/UseAdminSafeOrganizationName";

import SubmissionTable from "./SubmissionTable";

describe("SubmissionTable", () => {
    test("renders a placeholder", async () => {
        mockSessionContext.mockReturnValue({
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "testSender",
            },
            dispatch: () => {},
            initialized: true,
            isUserAdmin: false,
            isUserReceiver: false,
            isUserSender: true,
            environment: "test",
        });
        const fixtures: Fixture[] = [
            {
                endpoint: SubmissionsResource.list(),
                args: [
                    {
                        organization: "testOrg",
                        cursor: "3000-01-01T00:00:00.000Z",
                        since: "2000-01-01T00:00:00.000Z",
                        until: "3000-01-01T00:00:00.000Z",
                        pageSize: 61,
                        sortdir: "DESC",
                        showFailed: false,
                    },
                ],
                error: false,
                response: [
                    { submissionId: 0 },
                    { submissionId: 1 },
                ] as SubmissionsResource[],
            },
        ];
        renderApp(<SubmissionTable />, { restHookFixtures: fixtures });

        const pagination = await screen.findByLabelText(
            /submissions pagination/i,
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

    describe("when rendering as an admin", () => {
        beforeEach(() => {
            mockSessionContext.mockReturnValue({
                activeMembership: {
                    memberType: MemberType.PRIME_ADMIN,
                    parsedName: Organizations.PRIMEADMINS,
                    service: "",
                },
                dispatch: () => {},
                initialized: true,
                isUserAdmin: true,
                isUserReceiver: false,
                isUserSender: false,
                environment: "test",
            });

            renderApp(<SubmissionTable />, { restHookFixtures: [] });
        });

        test("renders a warning about not being able to request submission history", async () => {
            expect(
                await screen.findByText(
                    "Cannot fetch Organization data as admin",
                ),
            ).toBeVisible();
            expect(
                await screen.findByText("Please try again as an Organization"),
            ).toBeVisible();
        });
    });
});
