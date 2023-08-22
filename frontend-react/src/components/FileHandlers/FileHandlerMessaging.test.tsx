import { screen, within } from "@testing-library/react";
import React from "react";

import { renderApp } from "../../utils/CustomRenderUtils";
import { Destination } from "../../resources/ActionDetailsResource";
import { conditionallySuppressConsole } from "../../utils/TestUtils";
import { ErrorCode, ResponseError } from "../../config/endpoints/waters";
import { FileType } from "../../utils/TemporarySettingsAPITypes";

import {
    RequestLevel,
    FileQualityFilterDisplay,
    RequestedChangesDisplay,
    ValidationErrorMessageProps,
    ValidationErrorMessage,
    getSafeFileName,
} from "./FileHandlerMessaging";

describe("RequestedChangesDisplay", () => {
    test("renders expected content", async () => {
        const restore = conditionallySuppressConsole("failure:");
        renderApp(
            <RequestedChangesDisplay
                title={RequestLevel.WARNING}
                heading={"THE HEADING"}
                message={"Broken Glass, Everywhere"}
                data={[]}
                schemaColumnHeader={FileType.CSV}
            />,
        );

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveClass("usa-alert--warning");

        const message = await screen.findByText("Broken Glass, Everywhere");
        expect(message).toHaveClass("usa-alert__text"); // imperfect, just want to make sure it's there

        const heading = await screen.findByText("THE HEADING");
        expect(heading).toHaveClass("usa-alert__heading"); // imperfect, just want to make sure it's there

        const table = screen.queryByRole("table");
        expect(table).not.toBeInTheDocument();
        restore();
    });

    test("renders table when data is given", async () => {
        const restore = conditionallySuppressConsole("failure:");
        // implicitly testing message truncation functionality here as well
        const fakeError1: ResponseError = {
            message: "first field error",
            indices: [1, 10, 100],
            field: "first field",
            trackingIds: ["first_id"],
            scope: "unclear",
            errorCode: ErrorCode.UNKNOWN,
            details: "none",
        };
        const fakeError2: ResponseError = {
            message: "second field error",
            indices: [2],
            field: "second field",
            trackingIds: ["second_id"],
            scope: "unclear",
            errorCode: ErrorCode.UNKNOWN,
            details: "none",
        };
        const fakeError3: ResponseError = {
            message: "third field error",
            indices: [3],
            field: "third field",
            trackingIds: ["third_id"],
            scope: "unclear",
            errorCode: ErrorCode.UNKNOWN,
            details: "none",
        };
        const fakeError4: ResponseError = {
            message: "fourth field error",
            indices: [4],
            field: "fourth field",
            trackingIds: ["fourth_id"],
            scope: "unclear",
            errorCode: ErrorCode.UNKNOWN,
            details: "none",
        };

        const errors = [fakeError1, fakeError2, fakeError3, fakeError4];
        renderApp(
            <RequestedChangesDisplay
                title={RequestLevel.ERROR}
                heading={"THE HEADING"}
                message={"Broken Glass, Everywhere"}
                data={errors}
                schemaColumnHeader={FileType.CSV}
            />,
        );

        const table = screen.queryByRole("table");
        expect(table).toBeInTheDocument();

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(5); // 3 errors + header

        const firstCells = await within(rows[1]).findAllByRole("cell");
        expect(firstCells).toHaveLength(2);
        expect(firstCells[0]).toHaveTextContent("first field error");
        expect(firstCells[1]).toHaveTextContent("1 + 10 + 100");

        const secondCells = await within(rows[2]).findAllByRole("cell");
        expect(secondCells[0]).toHaveTextContent("second field error");

        const thirdCells = await within(rows[3]).findAllByRole("cell");
        expect(thirdCells[0]).toHaveTextContent("third field error");

        const fourthCells = await within(rows[4]).findAllByRole("cell");
        expect(fourthCells[0]).toHaveTextContent("fourth field error");
        restore();
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
        renderApp(
            <FileQualityFilterDisplay
                destinations={qualityFilterMessages}
                heading={""}
                message={
                    "The following records were filtered out while processing/validating your file."
                }
            />,
        );

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveClass("usa-alert--warning");

        const message = await screen.findByText(
            "The following records were filtered out while processing/validating your file.",
        );
        expect(message).toHaveClass("usa-alert__text"); // imperfect, just want to make sure it's there

        const table = await screen.findAllByRole("table");
        expect(table).toHaveLength(2);

        const rows = await screen.findAllByRole("row");
        expect(rows).toHaveLength(6);

        expect(
            screen.queryByText(/Maryland Public Health Department/),
        ).not.toBeInTheDocument();
        expect(
            screen.getByText(/Alaska Public Health Department/),
        ).toBeInTheDocument();
        const row1 = await within(rows[1]).findAllByRole("cell");
        expect(row1[0]).toHaveTextContent("Filtered out item Alaska1");
        expect(
            screen.getByText(/Hawaii Public Health Department/),
        ).toBeInTheDocument();
        const row3 = await within(rows[3]).findAllByRole("cell");
        expect(row3[0]).toHaveTextContent("Filtered out item Hawaii6");
    });
});

describe("ValidationErrorMessage", () => {
    const DEFAULT_PROPS: ValidationErrorMessageProps = {
        errorCode: ErrorCode.UNKNOWN,
        field: "validation_field",
        message: "default validation message",
    };

    let errorMessageNode: HTMLElement;

    function renderComponent(props: Partial<ValidationErrorMessageProps>) {
        const view = renderApp(
            <ValidationErrorMessage {...DEFAULT_PROPS} {...props} />,
        );

        errorMessageNode = screen.getByTestId("ValidationErrorMessage");

        return view;
    }

    describe("when the error code is INVALID_MSG_PARSE_BLANK", () => {
        beforeEach(() => {
            renderComponent({ errorCode: ErrorCode.INVALID_MSG_PARSE_BLANK });
        });

        test("renders an error about a blank message", () => {
            expect(errorMessageNode).toHaveTextContent(
                "Blank message(s) found within file. Blank messages cannot be processed.",
            );
        });
    });

    describe("when the error code is INVALID_HL7_MSG_TYPE_MISSING", () => {
        beforeEach(() => {
            renderComponent({
                errorCode: ErrorCode.INVALID_HL7_MSG_TYPE_MISSING,
            });
        });

        test("renders an error about a missing message type field", () => {
            expect(errorMessageNode).toHaveTextContent(
                "Missing required HL7 message type field MSH-9. Fill in the blank field before resubmitting.",
            );
        });
    });

    describe("when the error code is INVALID_HL7_MSG_TYPE_UNSUPPORTED", () => {
        beforeEach(() => {
            renderComponent({
                errorCode: ErrorCode.INVALID_HL7_MSG_TYPE_UNSUPPORTED,
            });
        });

        test("renders an error about an unsupported type", () => {
            expect(errorMessageNode).toHaveTextContent(
                "We found an unsupported HL7 message type. Please reformat to ORU-RO1. Refer to HL7 specification for more details.",
            );
        });

        test("renders a link to the HL7 product matrix", () => {
            expect(screen.getByRole("link")).toHaveAttribute(
                "href",
                "https://www.hl7.org/implement/standards/product_brief.cfm",
            );
        });
    });

    describe("when the error is INVALID_HL7_MSG_FORMAT_INVALID", () => {
        beforeEach(() => {
            renderComponent({
                errorCode: ErrorCode.INVALID_HL7_MSG_FORMAT_INVALID,
            });
        });

        test("renders an error about an invalid format", () => {
            expect(errorMessageNode).toHaveTextContent(
                "Invalid HL7 message format. Check your formatting by referring to HL7 specification.",
            );
        });

        test("renders a link to the HL7 product matrix", () => {
            expect(screen.getByRole("link")).toHaveAttribute(
                "href",
                "https://www.hl7.org/implement/standards/product_brief.cfm",
            );
        });
    });

    describe("when the error is INVALID_MSG_PARSE_DATETIME", () => {
        beforeEach(() => {
            renderComponent({
                errorCode: ErrorCode.INVALID_MSG_PARSE_DATETIME,
            });
        });

        test("renders an error about an invalid datetime", () => {
            expect(errorMessageNode).toHaveTextContent(
                "Reformat validation_field as YYYYMMDDHHMM[SS[.S[S[S[S]+/-ZZZZ.",
            );
        });
    });

    describe("when the error is INVALID_MSG_PARSE_TELEPHONE", () => {
        beforeEach(() => {
            renderComponent({
                errorCode: ErrorCode.INVALID_MSG_PARSE_TELEPHONE,
            });
        });

        test("renders an error about an invalid phone number", () => {
            expect(errorMessageNode).toHaveTextContent(
                "Reformat phone number to a 10-digit phone number (e.g. (555) 555-5555).",
            );
        });
    });

    describe("when the error is INVALID_HL7_MSG_VALIDATION", () => {
        beforeEach(() => {
            renderComponent({
                errorCode: ErrorCode.INVALID_HL7_MSG_VALIDATION,
            });
        });

        test("renders an error about an invalid field ", () => {
            expect(errorMessageNode).toHaveTextContent(
                "Reformat validation_field to HL7 specification.",
            );
        });

        test("renders a link to the HL7 product matrix", () => {
            expect(screen.getByRole("link")).toHaveAttribute(
                "href",
                "https://www.hl7.org/implement/standards/product_brief.cfm",
            );
        });
    });

    describe("when the error is INVALID_MSG_MISSING_FIELD", () => {
        beforeEach(() => {
            renderComponent({ errorCode: ErrorCode.INVALID_MSG_MISSING_FIELD });
        });

        test("renders an error about a missing field", () => {
            expect(errorMessageNode).toHaveTextContent(
                "Fill in the required field validation_field.",
            );
        });
    });

    describe("when the error is INVALID_MSG_EQUIPMENT_MAPPING", () => {
        beforeEach(() => {
            renderComponent({
                errorCode: ErrorCode.INVALID_MSG_EQUIPMENT_MAPPING,
            });
        });

        test("renders an error about LIVD table LOINC mapping", () => {
            expect(errorMessageNode).toHaveTextContent(
                "Reformat field validation_field. Refer to CDC LIVD table LOINC mapping spreadsheet for acceptable values.",
            );
        });

        test("renders a link to the CDC LIVD table LOINC mapping spreadsheet", () => {
            expect(screen.getByRole("link")).toHaveAttribute(
                "href",
                "https://www.cdc.gov/csels/dls/livd-codes.html",
            );
        });
    });

    describe("when the error is UNKNOWN", () => {
        describe("when the message is provided", () => {
            beforeEach(() => {
                renderComponent({
                    errorCode: ErrorCode.UNKNOWN,
                    message: "this is wrong please fix it kthxbai",
                });
            });

            test("renders the message", () => {
                expect(errorMessageNode).toHaveTextContent(
                    "this is wrong please fix it kthxbai",
                );
            });
        });

        describe("when the message is not provided", () => {
            beforeEach(() => {
                renderComponent({
                    errorCode: ErrorCode.UNKNOWN,
                    message: undefined,
                });
            });

            // TODO: check with Audrey if there should be a fallback message
            test("renders an empty string", () => {
                expect(errorMessageNode).toHaveTextContent("");
            });
        });
    });
});

describe("getSafeFileName", () => {
    test("returns a safe file name, replacing non-alphanumeric characters with hyphens", () => {
        expect(getSafeFileName("aaa", RequestLevel.WARNING)).toEqual(
            "aaa-warnings",
        );
        expect(getSafeFileName("aaa-!@#.csv", RequestLevel.WARNING)).toEqual(
            "aaa-----csv-warnings",
        );
        expect(
            getSafeFileName("Hello I Am A File", RequestLevel.WARNING),
        ).toEqual("hello-i-am-a-file-warnings");

        expect(getSafeFileName("aaa", RequestLevel.ERROR)).toEqual(
            "aaa-errors",
        );
        expect(getSafeFileName("aaa!@#.csv", RequestLevel.ERROR)).toEqual(
            "aaa----csv-errors",
        );
        expect(
            getSafeFileName("Hello I Am A File", RequestLevel.ERROR),
        ).toEqual("hello-i-am-a-file-errors");
    });
});
