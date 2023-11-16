import { screen } from "@testing-library/react";

import { FacilityResource } from "../../../config/endpoints/dataDashboard";
import {
    orgServer,
    receiversGenerator,
} from "../../../__mocks__/OrganizationMockServer";
import { mockUseOrganizationReceiversFeed } from "../../../hooks/network/Organizations/__mocks__/ReceiversHooks";
import { mockFilterManager } from "../../../hooks/filters/mocks/MockFilterManager";
import { makeRSReceiverSubmitterResponseFixture } from "../../../__mocks__/DataDashboardMockServer";
import { mockUseReceiverSubmitter } from "../../../hooks/network/DataDashboard/__mocks__/UseReceiverSubmitter";
import { MemberType } from "../../../utils/OrganizationUtils";
import { render } from "../../../utils/Test/render";

import FacilitiesProvidersTable from "./FacilitiesProvidersTable";

const mockData: FacilityResource[] = [
    {
        facilityId: "12w3e4r5",
        name: "Sally Doctor",
        location: "San Diego, CA",
        facilityType: "provider",
        reportDate: "2022-09-28T22:21:33.801667",
    },
    {
        facilityId: "12w3e4r6",
        name: "AFC Urgent Care",
        location: "San Antonio, TX",
        facilityType: "facility",
        reportDate: "2022-09-28T22:21:33.801667",
    },
    {
        facilityId: "12w3e4r7",
        name: "SimpleReport",
        location: "Fairfield, CO",
        facilityType: "submitter",
        reportDate: "2022-09-28T22:21:33.801667",
    },
];

const mockReceivers = receiversGenerator(5);
const mockActiveReceiver = mockReceivers[0];

vi.mock("rest-hooks", async () => ({
    ...(await vi.importActual<typeof import("rest-hooks")>("rest-hooks")),
    useResource: () => {
        return mockData;
    },
    useController: () => {
        // fetch is destructured as fetchController in component
        return { fetch: () => mockData };
    },
    // Must return children when mocking, otherwise nothing inside renders
    NetworkErrorBoundary: ({ children }: { children: JSX.Element[] }) => {
        return <>{children}</>;
    },
}));

describe("FacilitiesProvidersTable", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    describe("useOrganizationReceiversFeed without data", () => {
        function setup() {
            // Mock our receiverServices feed data
            mockUseOrganizationReceiversFeed.mockReturnValue({
                activeService: undefined,
                isLoading: false,
                data: [],
                setActiveService: () => {},
                isDisabled: false,
            } as any);

            // Mock the response from the Submitters hook
            const mockUseReceiverSubmitterCallback = {
                data: makeRSReceiverSubmitterResponseFixture(10),
                filterManager: mockFilterManager,
                isLoading: false,
            };
            mockUseReceiverSubmitter.mockReturnValue(
                mockUseReceiverSubmitterCallback as any,
            );

            // Render the component
            render(<FacilitiesProvidersTable />, {
                providers: {
                    Session: {
                        authState: {
                            accessToken: { accessToken: "TOKEN" },
                        } as any,
                        activeMembership: {
                            memberType: MemberType.RECEIVER,
                            parsedName: "testOrgNoReceivers",
                            service: "testReceiver",
                        },

                        user: {
                            isUserAdmin: false,
                            isUserReceiver: true,
                            isUserSender: false,
                            isUserTransceiver: false,
                        } as any,
                    },
                },
            });
        }

        test("if no active service display NoServicesBanner", async () => {
            setup();
            const heading = await screen.findByText(/No available data/i);
            expect(heading).toBeInTheDocument();
        });
    });
});

describe("FacilitiesProvidersTable", () => {
    describe("with receiver services and data", () => {
        function setup() {
            mockUseOrganizationReceiversFeed.mockReturnValue({
                activeService: mockActiveReceiver,
                isLoading: false,
                data: mockReceivers,
                setActiveService: () => {},
                isDisabled: false,
            } as any);

            const mockUseReceiverSubmitterCallback = {
                data: makeRSReceiverSubmitterResponseFixture(1),
                filterManager: mockFilterManager,
                isLoading: false,
            };
            mockUseReceiverSubmitter.mockReturnValue(
                mockUseReceiverSubmitterCallback as any,
            );

            // Render the component
            render(<FacilitiesProvidersTable />, {
                providers: {
                    Session: {
                        authState: {
                            accessToken: { accessToken: "TOKEN" },
                        } as any,
                        activeMembership: {
                            memberType: MemberType.RECEIVER,
                            parsedName: "testOrgNoReceivers",
                            service: "testReceiver",
                        },
                        user: {
                            isUserAdmin: false,
                            isUserReceiver: true,
                            isUserSender: false,
                            isUserTransceiver: false,
                        } as any,
                    },
                },
            });
        }

        test("renders with no error", async () => {
            setup();
            // Column headers render
            expect(screen.getByText("Name")).toBeInTheDocument();
            expect(screen.getByText("Location")).toBeInTheDocument();
            expect(screen.getByText("Facility type")).toBeInTheDocument();
            expect(
                screen.getByText("Most recent report date"),
            ).toBeInTheDocument();
        });

        test("renders Facility type column with transformed name", async () => {
            setup();
            expect(screen.getAllByText("SUBMITTER")[0]).toBeInTheDocument();
        });
    });
});
