import { screen, within } from "@testing-library/react";

import { Organizations } from "../../hooks/UseAdminSafeOrganizationName";
import { MemberType } from "../../utils/OrganizationUtils";
import { render } from "../../utils/Test/render";
import usePagination from "../../hooks/UsePagination";

import SubmissionTable from "./SubmissionTable";

vi.mock("../../hooks/UsePagination");

describe("SubmissionTable", () => {
    const mockUsePagination = vi.mocked(usePagination);

    test("renders a placeholder", async () => {
        mockUsePagination.mockReturnValue({
            currentPageResults: [{ submissionId: 0 }, { submissionId: 1 }],
            isLoading: false,
            paginationProps: {
                currentPageNum: 1,
                setSelectedPage: vi.fn(),
                slots: [],
                label: "",
            },
        });
        render(<SubmissionTable />, {
            providers: {
                Session: {
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
                },
            },
        });

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
        function setup() {
            mockUsePagination.mockReturnValue({
                currentPageResults: [],
                isLoading: false,
                paginationProps: {
                    currentPageNum: 1,
                    setSelectedPage: vi.fn(),
                    slots: [],
                    label: "",
                },
            });
            render(<SubmissionTable />, {
                providers: {
                    Session: {
                        activeMembership: {
                            memberType: MemberType.PRIME_ADMIN,
                            parsedName: Organizations.PRIMEADMINS,
                            service: "",
                        },
                        user: {
                            isAdmin: true,
                            isReceiver: false,
                            isSender: false,
                            isTransceiver: false,
                        } as any,
                    },
                },
            });
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
