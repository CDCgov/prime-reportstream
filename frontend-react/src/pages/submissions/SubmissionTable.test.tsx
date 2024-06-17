import { Fixture } from "@rest-hooks/test";
import { screen, within } from "@testing-library/react";

import SubmissionTable from "./SubmissionTable";
import { Organizations } from "../../hooks/UseAdminSafeOrganizationName/UseAdminSafeOrganizationName";
import SubmissionsResource from "../../resources/SubmissionsResource";
import { renderApp } from "../../utils/CustomRenderUtils";
import { MemberType } from "../../utils/OrganizationUtils";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../contexts/Session/__mocks__/useSessionContext")
>("../../contexts/Session/useSessionContext");

describe("SubmissionTable", () => {
    test("renders a placeholder", async () => {
        mockSessionContentReturnValue({
            activeMembership: {
                memberType: MemberType.SENDER,
                parsedName: "testOrg",
                service: "testSender",
            },

            user: {
                isUserAdmin: false,
                isUserReceiver: false,
                isUserSender: true,
                isUserTransceiver: false,
            } as any,
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
                    {
                        submissionId: 0,
                        timestamp: "2024-04-01T16:55:28.012Z",
                        sender: "ignore.ignore-elr-elims",
                        httpStatus: 201,
                        id: "3fc6bc2b-91e0-44f0-a73e-5bead6291061",
                        topic: "elr-elims",
                        reportItemCount: 1,
                        fileName:
                            "None-3fc6bc2b-91e0-44f0-a73e-5bead6291061-20240401165526.hl7",
                        fileType: "HL7",
                        externalName: "myfile.hl7",
                    },
                    {
                        submissionId: 1,
                        timestamp: "2024-03-14T17:40:50.641Z",
                        sender: "ignore.ignore-elr-elims",
                        httpStatus: 201,
                        id: "03c3b7ab-7c65-4174-bea7-9195cbb7ed01",
                        topic: "elr-elims",
                        reportItemCount: 1,
                        fileName:
                            "None-03c3b7ab-7c65-4174-bea7-9195cbb7ed01-20240314174050.hl7",
                        fileType: "HL7",
                    },
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

        const externalFileName = screen.getByText(/myfile.hl7/i);
        expect(externalFileName).toBeInTheDocument();
        const fileName = screen.getByText(
            /None-03c3b7ab-7c65-4174-bea7-9195cbb7ed01-20240314174050.hl7/i,
        );
        expect(fileName).toBeInTheDocument();
    });

    describe("when rendering as an admin", () => {
        function setup() {
            mockSessionContentReturnValue({
                activeMembership: {
                    memberType: MemberType.PRIME_ADMIN,
                    parsedName: Organizations.PRIMEADMINS,
                    service: "",
                },
                user: {
                    isUserAdmin: true,
                    isUserReceiver: false,
                    isUserSender: false,
                    isUserTransceiver: false,
                } as any,
            });

            renderApp(<SubmissionTable />, { restHookFixtures: [] });
        }

        test("renders a warning about not being able to request submission history", async () => {
            setup();
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
