import { screen } from "@testing-library/react";

import { receiversGenerator } from "../../../__mocks__/OrganizationMockServer";
import { mockFilterManager } from "../../../hooks/filters/mocks/MockFilterManager";
import { makeRSReceiverSubmitterResponseFixture } from "../../../__mocks__/DataDashboardMockServer";
import { render } from "../../../utils/Test/render";

import FacilitiesProvidersTable from "./FacilitiesProvidersTable";

const mockReceivers = receiversGenerator(5);
const mockActiveReceiver = mockReceivers[0];

describe("FacilitiesProvidersTable", () => {
    describe("with receiver services and data", () => {
        const { data: submitters, meta } =
                makeRSReceiverSubmitterResponseFixture(1),
            filterManager = mockFilterManager;
        function setup() {
            // Render the component
            render(
                <FacilitiesProvidersTable
                    activeService={mockActiveReceiver}
                    submitters={submitters}
                    filterManager={filterManager}
                    onFilterClick={() => void 0}
                    receiverServices={mockReceivers}
                    setActiveService={() => void 0}
                    pagesTotal={meta.totalFilteredCount}
                    submittersTotal={meta.totalCount}
                />,
            );
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
