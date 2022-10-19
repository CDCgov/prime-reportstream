import { render, screen, within } from "@testing-library/react";

import { QualityFilters } from "./QualityFilters";

describe("QualityFilters component", () => {
    test("renders expected content", async () => {
        const qualityFilters = [
            {
                organization: "Alaska Public Health Department",
                organization_id: "ak-phd",
                service: "elr",
                itemCount: 2,
                itemCountBeforeQualityFiltering: 5,
                filteredReportRows: [
                    "Filtered out item Alaska1",
                    "Filtered out item Alaska2",
                    "Filtered out item Alaska4",
                ],
                filteredReportItems: [
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "hasValidDataFor",
                        filteredTrackingElement: "Alaska1",
                        filterArgs: ["patient_dob"],
                        message: "Filtered out item Alaska1",
                    },
                ],
                sentReports: [],
                sending_at: "",
            },
            {
                organization: "Hawaii Public Health Department",
                organization_id: "hi-phd",
                service: "elr",
                itemCount: 2,
                itemCountBeforeQualityFiltering: 5,
                filteredReportRows: [
                    "Filtered out item Hawaii6",
                    "Filtered out item Hawaii7",
                    "Filtered out item Hawaii9",
                ],
                filteredReportItems: [
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "hasValidDataFor",
                        filteredTrackingElement: "Hawaii6",
                        filterArgs: ["specimen_type"],
                        message: "Filtered out item Hawaii6",
                    },
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "hasValidDataFor",
                        filteredTrackingElement: "Hawaii7",
                        filterArgs: ["specimen_type"],
                        message: "Filtered out item Hawaii7",
                    },
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "hasValidDataFor",
                        filteredTrackingElement: "Hawaii9",
                        filterArgs: ["specimen_type"],
                        message: "Filtered out item Hawaii9",
                    },
                ],
                sentReports: [],
                sending_at: "",
            },
        ];
        render(<QualityFilters qualityFilters={qualityFilters} />);

        expect(screen.getByText(/Jurisdictions:/)).toBeInTheDocument();

        const table = await screen.findAllByRole("table");
        expect(table).toHaveLength(2);

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(6);

        const headerCells = await within(rows[0]).findAllByRole("columnheader");
        expect(headerCells).toHaveLength(3);
        expect(headerCells[0]).toHaveTextContent(
            "Alaska Public Health Department"
        );
        expect(headerCells[1]).toHaveTextContent("Field");
        expect(headerCells[2]).toHaveTextContent("Tracking element");

        const firstCells = await within(rows[1]).findAllByRole("cell");
        expect(firstCells).toHaveLength(3);
        expect(firstCells[0]).toHaveTextContent("Filtered out item Alaska1");
        expect(firstCells[1]).toHaveTextContent("hasValidDataFor");
        expect(firstCells[2]).toHaveTextContent("Alaska1");
    });
});
