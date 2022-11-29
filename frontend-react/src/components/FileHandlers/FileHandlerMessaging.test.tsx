import { render, screen, within } from "@testing-library/react";

import {
    renderWithFullAppContext,
    renderWithRouter,
} from "../../utils/CustomRenderUtils";
import { formattedDateFromTimestamp } from "../../utils/DateTimeUtils";
import { Destination } from "../../resources/ActionDetailsResource";
import { ResponseError } from "../../config/endpoints/waters";

import {
    RequestLevel,
    FileQualityFilterDisplay,
    FileSuccessDisplay,
    RequestedChangesDisplay,
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
    test("renders expected content", async () => {
        renderWithFullAppContext(
            <RequestedChangesDisplay
                title={RequestLevel.WARNING}
                heading={"THE HEADING"}
                message={"Broken Glass, Everywhere"}
                data={[]}
                handlerType={""}
            />
        );

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveClass("usa-alert--warning");

        const message = await screen.findByText("Broken Glass, Everywhere");
        expect(message).toHaveClass("usa-alert__text"); // imperfect, just want to make sure it's there

        const heading = await screen.findByText("THE HEADING");
        expect(heading).toHaveClass("usa-alert__heading"); // imperfect, just want to make sure it's there

        const table = screen.queryByRole("table");
        expect(table).not.toBeInTheDocument();
    });

    test("renders table when data is given", async () => {
        // implicitly testing message truncation functionality here as well
        const fakeError1: ResponseError = {
            message: "Exception: first error\ntruncated",
            indices: [1],
            field: "first field",
            trackingIds: ["first_id"],
            scope: "unclear",
            errorCode: "INVALID_HL7_MESSAGE_DATE_VALIDATION",
            details: "none",
        };
        const fakeError2: ResponseError = {
            message: "Exception: second error\ntruncated",
            indices: [2],
            field: "second field",
            trackingIds: ["second_id"],
            scope: "unclear",
            errorCode: "INVALID_HL7_MESSAGE_VALIDATION",
            details: "none",
        };
        const fakeError3: ResponseError = {
            message: "Exception: third error\ntruncated",
            indices: [3],
            field: "third field",
            trackingIds: ["third_id"],
            scope: "unclear",
            errorCode: "INVALID_HL7_PHONE_NUMBER",
            details: "none",
        };
        const errors = [fakeError1, fakeError2, fakeError3];
        renderWithFullAppContext(
            <RequestedChangesDisplay
                title={RequestLevel.ERROR}
                heading={"THE HEADING"}
                message={"Broken Glass, Everywhere"}
                data={errors}
                handlerType={""}
            />
        );

        const table = screen.queryByRole("table");
        expect(table).toBeInTheDocument();

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(4); // 3 errors + header

        const firstCells = await within(rows[1]).findAllByRole("cell");
        expect(firstCells).toHaveLength(3);
        expect(firstCells[0]).toHaveTextContent(
            "Invalid entry for field. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format."
        );
        expect(firstCells[1]).toHaveTextContent("first field");
        expect(firstCells[2]).toHaveTextContent("first_id");

        const secondCells = await within(rows[2]).findAllByRole("cell");
        expect(secondCells[0]).toHaveTextContent("Invalid entry for field.");

        const thirdCells = await within(rows[3]).findAllByRole("cell");
        expect(thirdCells[0]).toHaveTextContent(
            "The string supplied is not a valid phone number. Reformat to a 10-digit phone number (e.g. (555) 555-5555)."
        );
    });
});

describe("FileQualityFilterDisplay", () => {
    test("renders expected content", async () => {
        const qualityFilterMessages: Destination[] = [
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
        render(
            <FileQualityFilterDisplay
                destinations={qualityFilterMessages}
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
        expect(table).toHaveLength(2);

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(6);

        expect(
            screen.queryByText(/Maryland Public Health Department/)
        ).not.toBeInTheDocument();
        expect(
            screen.getByText(/Alaska Public Health Department/)
        ).toBeInTheDocument();
        const row1 = await within(rows[1]).findAllByRole("cell");
        expect(row1[0]).toHaveTextContent("Filtered out item Alaska1");
        expect(
            screen.getByText(/Hawaii Public Health Department/)
        ).toBeInTheDocument();
        const row3 = await within(rows[3]).findAllByRole("cell");
        expect(row3[0]).toHaveTextContent("Filtered out item Hawaii6");
    });
});
