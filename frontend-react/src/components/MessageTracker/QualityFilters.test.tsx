import { render, screen, within } from "@testing-library/react";

import { QualityFilters } from "./QualityFilters";

describe("QualityFilters component", () => {
    test("renders expected content", async () => {
        const qualityFilters = [
            {
                trackingId: "Alaska1",
                detail: {
                    class: "gov.cdc.prime.router.ReportStreamFilterResult",
                    receiverName: "ak-phd.elr",
                    originalCount: 5,
                    filterName: "isValidCLIA",
                    filterArgs: ["testing_lab_clia", "reporting_facility_clia"],
                    filteredTrackingElement: "Alaska1",
                    filterType: "QUALITY_FILTER",
                    scope: "translation",
                    message:
                        "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska1",
                },
            },
            {
                trackingId: "Alaska2",
                detail: {
                    class: "gov.cdc.prime.router.ReportStreamFilterResult",
                    receiverName: "ak-phd.elr",
                    originalCount: 5,
                    filterName: "isValidCLIA",
                    filterArgs: ["testing_lab_clia", "reporting_facility_clia"],
                    filteredTrackingElement: "Alaska2",
                    filterType: "QUALITY_FILTER",
                    scope: "translation",
                    message:
                        "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska2",
                },
            },
        ];
        render(<QualityFilters qualityFilters={qualityFilters} />);

        expect(screen.getByText(/Quality Filters:/)).toBeInTheDocument();

        const table = await screen.findByRole("table");
        expect(table).toBeInTheDocument();

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(2);

        const firstRow = await within(rows[0]).findByRole("cell");
        expect(firstRow).toBeInTheDocument();
        expect(firstRow).toHaveTextContent(
            "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska1"
        );
        const secondRow = await within(rows[1]).findByRole("cell");
        expect(secondRow).toBeInTheDocument();
        expect(secondRow).toHaveTextContent(
            "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska2"
        );
    });
});
