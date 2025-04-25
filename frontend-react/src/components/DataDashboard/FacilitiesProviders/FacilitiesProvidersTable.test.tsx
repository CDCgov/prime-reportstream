import { screen } from "@testing-library/react";

import FacilitiesProvidersTable from "./FacilitiesProvidersTable";
import { makeRSReceiverSubmitterResponseFixture } from "../../../__mockServers__/DataDashboardMockServer";
import { orgServer, receiversGenerator } from "../../../__mockServers__/OrganizationMockServer";
import { FacilityResource } from "../../../config/endpoints/dataDashboard";
import useReceiverSubmitter from "../../../hooks/api/deliveries/UseReceiverSubmitters/UseReceiverSubmitters";
import useOrganizationReceivers from "../../../hooks/api/organizations/UseOrganizationReceivers/UseOrganizationReceivers";
import { filterManagerFixture } from "../../../hooks/filters/filters.fixtures";
import { renderApp } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../utils/OrganizationUtils";

vi.mock("../../../hooks/api/deliveries/UseReceiverSubmitters/UseReceiverSubmitters");
vi.mock("../../../hooks/api/organizations/UseOrganizationReceivers/UseOrganizationReceivers");

const mockUseReceiverSubmitter = vi.mocked(useReceiverSubmitter);
const mockUseOrganizationReceivers = vi.mocked(useOrganizationReceivers);
const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../../contexts/Session/__mocks__/useSessionContext")
>("../../../contexts/Session/useSessionContext");

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
const mockFilterManager = { ...filterManagerFixture };

vi.mock("rest-hooks", async (importActual) => ({
    ...(await importActual<typeof import("rest-hooks")>()),
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

describe("useOrganizationReceiversFeed", () => {
    beforeAll(() => orgServer.listen());
    afterEach(() => orgServer.resetHandlers());
    afterAll(() => orgServer.close());

    describe("useOrganizationReceivers without data", () => {
        function setup() {
            // Mock our receiverServices feed data
            mockUseOrganizationReceivers.mockReturnValue({
                allReceivers: [],
                activeReceivers: [],
                isLoading: false,
                isDisabled: false,
            } as any);

            // Mock our SessionProvider's data
            mockSessionContentReturnValue({
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
            });

            // Mock the response from the Submitters hook
            const mockUseReceiverSubmitterCallback = {
                data: makeRSReceiverSubmitterResponseFixture(10),
                filterManager: mockFilterManager,
                isLoading: false,
            };
            mockUseReceiverSubmitter.mockReturnValue(mockUseReceiverSubmitterCallback as any);

            // Render the component
            renderApp(<FacilitiesProvidersTable />);
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
            mockUseOrganizationReceivers.mockReturnValue({
                allReceivers: [mockActiveReceiver],
                activeReceivers: [mockActiveReceiver],
                isLoading: false,
            } as any);

            // Mock our SessionProvider's data
            mockSessionContentReturnValue({
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
            });

            const mockUseReceiverSubmitterCallback = {
                data: makeRSReceiverSubmitterResponseFixture(1),
                filterManager: mockFilterManager,
                isLoading: false,
            };
            mockUseReceiverSubmitter.mockReturnValue(mockUseReceiverSubmitterCallback as any);

            // Render the component
            renderApp(<FacilitiesProvidersTable />);
        }

        test("renders with no error", () => {
            setup();
            // Column headers render
            expect(screen.getByText("Name")).toBeInTheDocument();
            expect(screen.getByText("Location")).toBeInTheDocument();
            expect(screen.getByText("Facility type")).toBeInTheDocument();
            expect(screen.getByText("Most recent report date")).toBeInTheDocument();
        });

        test("renders Facility type column with transformed name", () => {
            setup();
            expect(screen.getAllByText("SUBMITTER")[0]).toBeInTheDocument();
        });
    });
});
