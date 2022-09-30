import { screen, render, within } from "@testing-library/react";

import { renderWithRouter } from "../../utils/CustomRenderUtils";
import { formattedDateFromTimestamp } from "../../utils/DateTimeUtils";
import { Destination } from "../../resources/ActionDetailsResource";

import {
    FileSuccessDisplay,
    FileErrorDisplay,
    FileWarningsDisplay,
    FileQualityFilterDisplay,
} from "./FileHandlerMessaging";

// Note: following a pattern of finding elements by text (often text passed as props)
// then asserting className. This is not ideal, and seems kinda backwards, but I couldn't
// think of a better way to do it. Is there a better pattern for this? - DWS
describe("FileSuccessDisplay", () => {
    test("renders expected content", async () => {
        renderWithRouter(
            <FileSuccessDisplay
                heading={"THE HEADING"}
                message={"Broken Glass, Everywhere"}
                showExtendedMetadata={true}
                extendedMetadata={{
                    destinations: "1, 2",
                    reportId: "IDIDID",
                    timestamp: new Date(0).toString(),
                }}
            />
        );

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveClass("usa-alert--success");

        const message = await screen.findByText("Broken Glass, Everywhere");
        expect(message).toHaveClass("usa-alert__text");

        const heading = await screen.findByText("THE HEADING");
        expect(heading).toHaveClass("usa-alert__heading");

        const destinations = await screen.findByText("1, 2");
        expect(destinations).toHaveClass("margin-top-05");

        const reportLink = await screen.findByRole("link");
        expect(reportLink).toHaveTextContent("IDIDID");
        expect(reportLink).toHaveAttribute("href", "/submissions/IDIDID");

        const timestampDate = await screen.findByText(
            formattedDateFromTimestamp(new Date(0).toString(), "DD MMMM YYYY")
        );
        expect(timestampDate).toHaveClass("margin-top-05");

        // // this may break if run outside of us east - commenting out until we confirm, or figure out a better way
        // const timestampTime = await screen.findByText("7:00 America/New_York");
        // expect(timestampTime).toHaveClass("margin-top-05");
    });
});

describe("FileErrorDisplay", () => {
    test("renders expected content (no error table)", async () => {
        render(
            <FileErrorDisplay
                fileName={"file.file"}
                heading={"THE HEADING"}
                message={"Broken Glass, Everywhere"}
                errors={[]}
                handlerType={""}
            />
        );

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveClass("usa-alert--error");

        const fileName = await screen.findByText("file.file");
        expect(fileName).toHaveClass("margin-top-05");

        const message = await screen.findByText("Broken Glass, Everywhere");
        expect(message).toHaveClass("usa-alert__text"); // imperfect, just want to make sure it's there

        const heading = await screen.findByText("THE HEADING");
        expect(heading).toHaveClass("usa-alert__heading"); // imperfect, just want to make sure it's there

        const table = screen.queryByRole("table");
        expect(table).not.toBeInTheDocument();
    });

    test("renders expected content (with error table)", async () => {
        // implicitly testing message truncation functionality here as well
        const errors = [
            {
                message: "Exception: first error\ntruncated",
                indices: [1],
                field: "first field",
                trackingIds: ["first_id"],
                scope: "unclear",
                details: "none",
            },
            {
                message: "Exception: second error\ntruncated",
                indices: [2],
                field: "second field",
                trackingIds: ["second_id"],
                scope: "unclear",
                details: "none",
            },
        ];
        render(
            <FileErrorDisplay
                fileName={"file.file"}
                heading={"THE HEADING"}
                message={"Broken Glass, Everywhere"}
                errors={errors}
                handlerType={""}
            />
        );

        const table = screen.queryByRole("table");
        expect(table).toBeInTheDocument();

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(3); // 2 errors + header

        const firstCells = await within(rows[1]).findAllByRole("cell");
        expect(firstCells).toHaveLength(4);
        expect(firstCells[0]).toHaveTextContent("Exception: first error");
        expect(firstCells[1]).toHaveTextContent("Row(s): 1");
        expect(firstCells[2]).toHaveTextContent("first field");
        expect(firstCells[3]).toHaveTextContent("first_id");
    });
});

describe("FileWarningsDisplay", () => {
    test("renders expected content", async () => {
        const warnings = [
            {
                message: "first warning",
                indices: [1],
                field: "first field",
                trackingIds: ["first_id"],
                scope: "unclear",
                details: "none",
            },
            {
                message: "second warning",
                indices: [2],
                field: "second field",
                trackingIds: ["second_id"],
                scope: "unclear",
                details: "none",
            },
        ];
        render(
            <FileWarningsDisplay
                heading={"THE HEADING"}
                message={"Broken Glass, Everywhere"}
                warnings={warnings}
            />
        );

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveClass("usa-alert--warning");

        const message = await screen.findByText("Broken Glass, Everywhere");
        expect(message).toHaveClass("usa-alert__text"); // imperfect, just want to make sure it's there

        const heading = await screen.findByText("THE HEADING");
        expect(heading).toHaveClass("usa-alert__heading"); // imperfect, just want to make sure it's there

        const table = screen.queryByRole("table");
        expect(table).toBeInTheDocument();

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(3); // 2 warnings + header

        const firstCells = await within(rows[1]).findAllByRole("cell");
        expect(firstCells).toHaveLength(4);
        expect(firstCells[0]).toHaveTextContent("first warning");
        expect(firstCells[1]).toHaveTextContent("Row(s): 1");
        expect(firstCells[2]).toHaveTextContent("first field");
        expect(firstCells[3]).toHaveTextContent("first_id");
    });
});

describe("FileQualityFilterDisplay", () => {
    test("renders expected content", async () => {
        const destinations: Destination[] = [
            {
                organization: "Maryland Public Health Department",
                organization_id: "md-phd",
                service: "elr",
                itemCount: 2,
                itemCountBeforeQualityFiltering: 2,
                filteredReportRows: [],
                filteredReportItems: [],
                sentReports: [],
                sending_at: "",
            },
            {
                organization: "Alaska Public Health Department",
                organization_id: "ak-phd",
                service: "elr",
                itemCount: 2,
                itemCountBeforeQualityFiltering: 5,
                filteredReportRows: [
                    "For ak-phd.elr, filter hasValidDataFor[patient_dob] filtered out item Alaska1",
                    "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska1",
                    "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska2",
                    "For ak-phd.elr, filter isValidCLIA[testing_lab_clia, reporting_facility_clia] filtered out item Alaska4",
                ],
                filteredReportItems: [
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "hasValidDataFor",
                        filteredTrackingElement: "Alaska1",
                        filterArgs: ["patient_dob"],
                        message: "Filtered out item Alaska1",
                    },
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "isValidCLIA",
                        filteredTrackingElement: "Alaska1",
                        filterArgs: [
                            "testing_lab_clia",
                            "reporting_facility_clia",
                        ],
                        message: "Filtered out item Alaska1",
                    },
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "isValidCLIA",
                        filteredTrackingElement: "Alaska2",
                        filterArgs: [
                            "testing_lab_clia",
                            "reporting_facility_clia",
                        ],
                        message: "Filtered out item Alaska2",
                    },
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "isValidCLIA",
                        filteredTrackingElement: "Alaska4",
                        filterArgs: [
                            "testing_lab_clia",
                            "reporting_facility_clia",
                        ],
                        message: "Filtered out item Alaska4",
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
                    "For hi-phd.elr, filter hasValidDataFor[specimen_type] filtered out item Hawaii6",
                    "For hi-phd.elr, filter hasValidDataFor[specimen_type] filtered out item Hawaii7",
                    "For hi-phd.elr, filter hasValidDataFor[specimen_type] filtered out item Hawaii9",
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
            {
                organization: "Ohio Department of Health",
                organization_id: "oh-doh",
                service: "elr",
                itemCount: 0,
                itemCountBeforeQualityFiltering: 2,
                filteredReportRows: [
                    "Filtered out item Ohio11",
                    "Filtered out item Ohio12",
                    "Filtered out item Ohio11",
                    "Filtered out item Ohio11",
                    "Filtered out item Ohio12",
                ],
                filteredReportItems: [
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "hasValidDataFor",
                        filteredTrackingElement: "Ohio11",
                        filterArgs: ["patient_dob"],
                        message: "Filtered out item Ohio11",
                    },
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "hasValidDataFor",
                        filteredTrackingElement: "Ohio12",
                        filterArgs: ["patient_dob"],
                        message: "Filtered out item Ohio12",
                    },
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "hasAtLeastOneOf",
                        filteredTrackingElement: "Ohio11",
                        filterArgs: [
                            "order_test_date",
                            "specimen_collection_date_time",
                            "test_result_date",
                        ],
                        message: "Filtered out item Ohio11",
                    },
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "isValidCLIA",
                        filteredTrackingElement: "Ohio11",
                        filterArgs: [
                            "testing_lab_clia",
                            "reporting_facility_clia",
                        ],
                        message: "Filtered out item Ohio11",
                    },
                    {
                        filterType: "QUALITY_FILTER",
                        filterName: "isValidCLIA",
                        filteredTrackingElement: "Ohio12",
                        filterArgs: [
                            "testing_lab_clia",
                            "reporting_facility_clia",
                        ],
                        message: "Filtered out item Ohio12",
                    },
                ],
                sentReports: [],
                sending_at: "",
            },
        ];
        render(
            <FileQualityFilterDisplay
                destinations={destinations}
                heading={""}
                message={
                    "The following records were filtered out while processing/validating your file."
                }
            />
        );

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveClass("usa-alert--error");

        const message = await screen.findByText(
            "The following records were filtered out while processing/validating your file."
        );
        expect(message).toHaveClass("usa-alert__text"); // imperfect, just want to make sure it's there

        const table = await screen.findAllByRole("table");
        expect(table).toHaveLength(3);

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(15);

        const row1 = await within(rows[1]).findAllByRole("cell");
        expect(row1[0]).toHaveTextContent("Filtered out item Alaska1");
        const row6 = await within(rows[6]).findAllByRole("cell");
        expect(row6[0]).toHaveTextContent("Filtered out item Hawaii6");
        const row10 = await within(rows[10]).findAllByRole("cell");
        expect(row10[0]).toHaveTextContent("Filtered out item Ohio11");
    });
});
