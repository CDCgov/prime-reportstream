import { screen } from "@testing-library/react";

import { mockSessionContext } from "../../../contexts/__mocks__/SessionContext";
import { renderWithRouter } from "../../../utils/CustomRenderUtils";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import { mockUseOrgDeliveries } from "../../../hooks/network/History/__mocks__/DeliveryHooks";
import { makeDeliveryFixtureArray } from "../../../__mocks__/DeliveriesMockServer";
import { RSService } from "../../../config/endpoints/services";

import DeliveriesTable from "./DeliveriesTable";

describe("DeliveriesTable", () => {
    beforeEach(() => {
        // Mock our SessionProvider's data
        mockSessionContext.mockReturnValue({
            oktaToken: {
                accessToken: "TOKEN",
            },
            activeMembership: {
                memberType: MemberType.RECEIVER,
                parsedName: "testOrg",
                service: "testReceiver",
                allServices: [
                    { name: "testReceiver" } as RSService,
                    { name: "testReceiver2" } as RSService,
                ],
            },
            initialized: true,
            dispatch: () => {},
        });
    });
    describe("with services and deliveries data", () => {
        beforeEach(() => {
            // Mock the response from the Deliveries hook
            mockUseOrgDeliveries.mockReturnValue({
                serviceReportsList: makeDeliveryFixtureArray(101),
            });
            // Render the component
            renderWithRouter(<DeliveriesTable />);
        });
        test("renders with no error", async () => {
            // Column headers render
            expect(await screen.findByText("Report ID")).toBeInTheDocument();
            expect(await screen.findByText("Available")).toBeInTheDocument();
            expect(await screen.findByText("Expires")).toBeInTheDocument();
            expect(await screen.findByText("Items")).toBeInTheDocument();
            expect(await screen.findByText("File")).toBeInTheDocument();
        });

        test("renders 100 results per page + 1 header row", () => {
            const rows = screen.getAllByRole("row");
            expect(rows).toHaveLength(100 + 1);
        });
    });
    describe("with no data", () => {
        beforeEach(() => {
            // Mock the response from the Deliveries hook
            mockUseOrgDeliveries.mockReturnValue({
                serviceReportsList: makeDeliveryFixtureArray(0),
            });
            // Render the component
            renderWithRouter(<DeliveriesTable />);
        });
        test("renders 0 results (but 1 header row)", () => {
            const rows = screen.getAllByRole("row");
            expect(rows.length).toBeLessThan(2);
            expect(rows.length).toBeGreaterThan(0);
        });
    });
});
