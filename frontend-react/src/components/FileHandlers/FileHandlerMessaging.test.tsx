import { screen, render, within } from "@testing-library/react";

import { formattedDateFromTimestamp } from "../../utils/DateTimeUtils";

import {
    FileSuccessDisplay,
    FileErrorDisplay,
    FileWarningsDisplay,
} from "./FileHandlerMessaging";

// Note: following a pattern of finding elements by text (often text passed as props)
// then asserting className. This is not ideal, and seems kinda backwards, but I couldn't
// think of a better way to do it. Is there a better pattern for this? - DWS
describe("FileSuccessDisplay", () => {
    test("renders expected content", async () => {
        render(
            <FileSuccessDisplay
                fileName={"file.file"}
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

        const fileName = await screen.findByText("file.file");
        expect(fileName).toHaveClass("margin-top-05");

        const message = await screen.findByText("Broken Glass, Everywhere");
        expect(message).toHaveClass("usa-alert__text");

        const heading = await screen.findByText("THE HEADING");
        expect(heading).toHaveClass("usa-alert__heading");

        const destinations = await screen.findByText("1, 2");
        expect(destinations).toHaveClass("margin-top-05");

        const reportId = await screen.findByText("IDIDID");
        expect(reportId).toHaveClass("margin-top-05");

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
