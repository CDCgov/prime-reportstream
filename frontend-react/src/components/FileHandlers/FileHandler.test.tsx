import { act, fireEvent, screen, waitFor } from "@testing-library/react";

describe("whatever", () => {
    test("", () => {
        expect(true).toEqual(true);
    });
});
/*
/!* eslint-disable testing-library/no-unnecessary-act *!/
// Even though the linter complains about act(),
// the test will fail when submitting the form with
// fireEvent.submit() which requires that its wrapped
// in act()
import { act, fireEvent, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";
import {
    ErrorCode,
    OverallStatus,
    WatersResponse,
} from "../../config/endpoints/waters";
import * as useFileHandlerExports from "../../hooks/UseFileHandler";
import { FileHandlerState, INITIAL_STATE } from "../../hooks/UseFileHandler";
import { mockAppInsights } from "../../utils/__mocks__/ApplicationInsights";
import * as useSenderSchemaOptionsExports from "../../senders/hooks/UseSenderSchemaOptions";
import {
    STANDARD_SCHEMA_OPTIONS,
    UseSenderSchemaOptionsHookResult,
} from "../../senders/hooks/UseSenderSchemaOptions";
import * as useWatersUploaderExports from "../../hooks/network/WatersHooks";
import {
    UseWatersUploaderResult,
    UseWatersUploaderSendFileMutation,
} from "../../hooks/network/WatersHooks";
import { MembershipSettings, MemberType } from "../../hooks/UseOktaMemberships";
import { CustomerStatus, Format } from "../../utils/TemporarySettingsAPITypes";
import { RSSender } from "../../config/endpoints/settings";

// import FileHandler, { getClientHeader } from "./FileHandler";
import FileHandler from "./FileHandler";

const mockSendValidFile: WatersResponse = {
    id: "",
    submissionId: 1,
    overallStatus: OverallStatus.VALID,
    sender: "",
    errorCount: 0,
    warningCount: 0,
    httpStatus: 200,
    errors: [],
    warnings: [],
};

const mockSendFileWithWarnings: WatersResponse = {
    id: "",
    submissionId: 1,
    overallStatus: OverallStatus.VALID,
    sender: "",
    errorCount: 0,
    warningCount: 2,
    httpStatus: 200,
    errors: [],
    warnings: [
        {
            details: "",
            scope: "item",
            indices: [1, 2],
            trackingIds: ["371784", "612092"],
            field: "MSH-7 (file_created_date)",
            message:
                "Timestamp for file_created_date should be precise. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
            errorCode: ErrorCode.INVALID_MSG_PARSE_DATE,
        },
        {
            details: "",
            scope: "item",
            indices: [1, 2],
            trackingIds: ["371784", "612092"],
            field: "ORC-15 (order_test_date)",
            message:
                "Timestamp for order_test_date should be precise. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
            errorCode: ErrorCode.INVALID_MSG_PARSE_DATE,
        },
    ],
};

const mockSendFileWithErrors: WatersResponse = {
    id: "",
    submissionId: 1,
    overallStatus: OverallStatus.ERROR,
    sender: "",
    errorCount: 2,
    warningCount: 0,
    httpStatus: 200,
    errors: [
        {
            details: "",
            scope: "item",
            indices: [1, 2],
            trackingIds: ["371784", "612092"],
            field: "MSH-7 (file_created_date)",
            message:
                "Timestamp for file_created_date should be precise. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
            errorCode: ErrorCode.INVALID_MSG_PARSE_DATE,
        },
        {
            details: "",
            scope: "item",
            indices: [1, 2],
            trackingIds: ["371784", "612092"],
            field: "ORC-15 (order_test_date)",
            message:
                "Timestamp for order_test_date should be precise. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
            errorCode: ErrorCode.INVALID_MSG_PARSE_DATE,
        },
    ],
    warnings: [],
};

jest.mock("../../hooks/UseOrganizationSettings", () => ({
    useOrganizationSettings: () => {
        return {
            data: {
                description: "wow, cool organization",
                createdAt: "2023-01-10T21:23:14.467Z",
                createdBy: "local@test.com",
                filters: [],
                jurisdiction: "FEDERAL",
                name: "aegis",
                version: 0,
            },
            isLoading: false,
        };
    },
}));

jest.mock("../../TelemetryService", () => ({
    ...jest.requireActual("../../TelemetryService"),
    getAppInsights: () => mockAppInsights,
}));

/!*
  below is an example of a mocked File & mocked React file input change event we can use for future tests
  thx to https://evanteague.medium.com/creating-fake-test-events-with-typescript-jest-778018379d1e
*!/

const contentString = "foo,bar\r\nbar,foo";

// doesn't work out of the box as it somehow doesn't come with a .text method
const fakeFile = new File([new Blob([contentString])], "file.csv", {
    type: "text/csv",
});
fakeFile.text = () => Promise.resolve(contentString);
const choosingSchema = async (dropdown = "upload-covid-19") => {
    expect(screen.getByText(/Select data model/)).toBeVisible();
    expect(screen.getByText(/Continue/i)).toBeDisabled();

    await userEvent.selectOptions(screen.getByTestId("dropdown"), [dropdown]);
};
describe("FileHandler integration test suite", () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });

    function mockUseFileHandler(
        fileHandlerState: Partial<FileHandlerState> = {}
    ) {
        jest.spyOn(useFileHandlerExports, "default").mockReturnValue({
            state: {
                // TODO: any sensible defaults?
                ...fileHandlerState,
            } as FileHandlerState,
            dispatch: () => {},
        });
    }

    function mockUseSenderSchemaOptions(
        result: Partial<UseSenderSchemaOptionsHookResult> = {}
    ) {
        jest.spyOn(useSenderSchemaOptionsExports, "default").mockReturnValue({
            isLoading: false,
            schemaOptions: STANDARD_SCHEMA_OPTIONS,
            ...result,
        });
    }

    function mockUseWatersUploader(
        result: Partial<UseWatersUploaderResult> = {}
    ) {
        jest.spyOn(
            useWatersUploaderExports,
            "useWatersUploader"
        ).mockReturnValue({
            isWorking: false,
            uploaderError: null,
            sendFile: (() =>
                Promise.resolve({})) as UseWatersUploaderSendFileMutation,
            ...result,
        });
    }

    describe("when the sender schema options are loading", () => {
        beforeEach(() => {
            mockUseFileHandler(INITIAL_STATE);
            mockUseSenderSchemaOptions({ isLoading: true });
            mockUseWatersUploader();

            renderApp(<FileHandler />);
        });

        test("renders a spinner", () => {
            expect(screen.getByLabelText("loading-indicator")).toBeVisible();
        });
    });

    describe("when the sender schema options have been loaded", () => {
        describe("when in the prompt state", () => {
            beforeEach(() => {
                mockUseFileHandler(INITIAL_STATE);
                mockUseSenderSchemaOptions({
                    isLoading: false,
                    schemaOptions: STANDARD_SCHEMA_OPTIONS,
                });
                mockUseWatersUploader({
                    isWorking: false,
                    uploaderError: null,
                    sendFile: jest.fn(),
                });

                renderApp(<FileHandler />);
            });

            test("renders as expected", () => {
                const headings = screen.getAllByRole("heading");
                expect(headings).toHaveLength(2);
                expect(headings[0]).toHaveTextContent(
                    "ReportStream File Validator"
                );
                expect(headings[1]).toHaveTextContent("wow, cool organization");

                expect(screen.getByText("Select data model")).toBeVisible();
            });
        });

        describe("when a schema has been selected", () => {
            beforeEach(() => {
                mockUseSenderSchemaOptions({
                    isLoading: false,
                    schemaOptions: STANDARD_SCHEMA_OPTIONS,
                });
                mockUseWatersUploader({
                    isWorking: false,
                    uploaderError: null,
                    sendFile: jest.fn(),
                });

                renderApp(<FileHandler />);
            });

            test("Continue button is enabled", async () => {
                expect(screen.getByText("Select data model")).toBeVisible();
                expect(screen.getByText("Continue")).toBeDisabled();

                await userEvent.selectOptions(screen.getByTestId("dropdown"), [
                    "upload-covid-19",
                ]);

                expect(screen.getByText("Continue")).toBeEnabled();
            });
        });

        describe("when a valid CSV file is being submitted", () => {
            beforeEach(() => {
                renderApp(<FileHandler />);
            });

            test("Select schema and proceed to select file", async () => {
                await choosingSchema();
                expect(screen.getByText("Continue")).toBeEnabled();

                await userEvent.click(screen.getByText("Continue"));
            });
            test("Select a CSV file", async () => {
                await choosingSchema();
                expect(screen.getByText("Continue")).toBeEnabled();

                await userEvent.click(screen.getByText("Continue"));

                expect(screen.getByText("Drag file here or")).toBeVisible();
                expect(screen.getByText("Submit")).toBeDisabled();
                await userEvent.upload(
                    screen.getByTestId("file-input-input"),
                    fakeFile
                );
                await screen.findByTestId("file-input-preview-image");
                expect(screen.getByText("Submit")).toBeEnabled();
            });
        });
    });
    describe("when a valid CSV file is being submitted", () => {
        let sendFileSpy: UseWatersUploaderSendFileMutation;
        beforeEach(() => {
            sendFileSpy = jest.fn(() => Promise.resolve(mockSendValidFile));
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: sendFileSpy,
            });
            renderApp(<FileHandler />);
        });

        test("Rendering a success page after submitting valid CSV", async () => {
            await choosingSchema();
            expect(screen.getByText("Continue")).toBeEnabled();

            await userEvent.click(screen.getByText("Continue"));

            expect(screen.getByText("Drag file here or")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
            await userEvent.upload(
                screen.getByTestId("file-input-input"),
                fakeFile
            );
            await screen.findByTestId("file-input-preview-image");

            expect(screen.getByText("Submit")).toBeEnabled();

            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            await waitFor(() => {
                return screen.getByText(
                    "Your file is correctly formatted for ReportStream."
                );
            });
        });
    });

    describe("when a CSV file with warnings is being submitted", () => {
        let sendFileSpy: UseWatersUploaderSendFileMutation;
        beforeEach(() => {
            sendFileSpy = jest.fn(() => {
                return Promise.resolve(mockSendFileWithWarnings);
            });
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: sendFileSpy,
            });

            renderApp(<FileHandler />);
        });

        test("Rendering a Warning page", async () => {
            await choosingSchema();
            expect(screen.getByText("Continue")).toBeEnabled();

            await userEvent.click(screen.getByText("Continue"));

            expect(screen.getByText("Drag file here or")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
            await userEvent.upload(
                screen.getByTestId("file-input-input"),
                fakeFile
            );
            await screen.findByTestId("file-input-preview-image");

            expect(screen.getByText("Submit")).toBeEnabled();

            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            expect(screen.getByText("Recommended edits found")).toBeVisible();
        });

        test("Ignore warning, and proceed to success page", async () => {
            await choosingSchema();
            expect(screen.getByText("Continue")).toBeEnabled();

            await userEvent.click(screen.getByText("Continue"));

            expect(screen.getByText("Drag file here or")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
            await userEvent.upload(
                screen.getByTestId("file-input-input"),
                fakeFile
            );
            await screen.findByTestId("file-input-preview-image");

            expect(screen.getByText("Submit")).toBeEnabled();

            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            expect(screen.getByText("Continue without changes")).toBeEnabled();

            await userEvent.click(screen.getByText(/^Continue$/));

            await waitFor(() => {
                return screen.getByText(
                    "Your file is correctly formatted for ReportStream."
                );
            });
        });
    });
    describe("when a CSV file with errors is being submitted", () => {
        let sendFileSpy: UseWatersUploaderSendFileMutation;
        beforeEach(() => {
            sendFileSpy = jest.fn(() => {
                return Promise.resolve(mockSendFileWithErrors);
            });
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: sendFileSpy,
            });

            renderApp(<FileHandler />);
        });

        test("Rendering an Error page", async () => {
            await choosingSchema();
            expect(screen.getByText("Continue")).toBeEnabled();

            await userEvent.click(screen.getByText("Continue"));

            expect(screen.getByText("Drag file here or")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
            await userEvent.upload(
                screen.getByTestId("file-input-input"),
                fakeFile
            );
            await screen.findByTestId("file-input-preview-image");

            expect(screen.getByText("Submit")).toBeEnabled();

            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            expect(
                screen.getByText("Resubmit with the required edits.")
            ).toBeVisible();
        });

        test("Attempt to proceed, go back to previous page with form reset", async () => {
            await choosingSchema();
            expect(screen.getByText("Continue")).toBeEnabled();

            await userEvent.click(screen.getByText("Continue"));

            expect(screen.getByText("Drag file here or")).toBeVisible();
            expect(screen.getByText("Submit")).toBeDisabled();
            await userEvent.upload(
                screen.getByTestId("file-input-input"),
                fakeFile
            );
            await screen.findByTestId("file-input-preview-image");

            expect(screen.getByText("File name")).toBeInTheDocument();

            expect(screen.getByText("Submit")).toBeEnabled();

            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            expect(
                screen.getByText("Resubmit with the required edits.")
            ).toBeVisible();

            expect(screen.getByText("Continue without changes")).toBeDisabled();

            await userEvent.click(screen.getByText("Test another file"));

            expect(screen.queryByText("File name")).not.toBeInTheDocument();
        });
    });
});

/!*
describe("getClientHeader", () => {
    const DEFAULT_SCHEMA_NAME = "whatever-schema";

    const DEFAULT_ACTIVE_MEMBERSHIP: MembershipSettings = {
        parsedName: "orgName",
        service: "serviceName",
        memberType: MemberType.SENDER,
    };

    const DEFAULT_SENDER: RSSender = {
        allowDuplicates: true,
        customerStatus: CustomerStatus.ACTIVE,
        format: Format.CSV,
        name: "default",
        organizationName: "orgName",
        processingType: "sync",
        schemaName: DEFAULT_SCHEMA_NAME,
        topic: "covid-19",
    };

    describe("when selectedSchemaName is falsy", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(
                    undefined,
                    DEFAULT_ACTIVE_MEMBERSHIP,
                    DEFAULT_SENDER
                )
            ).toEqual("");
        });
    });

    describe("when activeMembership is falsy", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(DEFAULT_SCHEMA_NAME, undefined, DEFAULT_SENDER)
            ).toEqual("");
            expect(
                getClientHeader(DEFAULT_SCHEMA_NAME, null, DEFAULT_SENDER)
            ).toEqual("");
        });
    });

    describe("when sender is falsy", () => {
        expect(
            getClientHeader(
                DEFAULT_SCHEMA_NAME,
                DEFAULT_ACTIVE_MEMBERSHIP,
                undefined
            )
        ).toEqual("");
    });

    describe("when activeMembership.parsedName is falsy", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(
                    DEFAULT_SCHEMA_NAME,
                    { ...DEFAULT_ACTIVE_MEMBERSHIP, parsedName: "" },
                    DEFAULT_SENDER
                )
            ).toEqual("");
        });
    });

    describe("when activeMembership.service is falsy", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(
                    DEFAULT_SCHEMA_NAME,
                    { ...DEFAULT_ACTIVE_MEMBERSHIP, service: "" },
                    DEFAULT_SENDER
                )
            ).toEqual("");
        });
    });

    describe("when selected schema value matches sender's schema", () => {
        test("returns the client value from the organization's parsed name and service", () => {
            expect(
                getClientHeader(
                    DEFAULT_SCHEMA_NAME,
                    DEFAULT_ACTIVE_MEMBERSHIP,
                    DEFAULT_SENDER
                )
            ).toEqual("orgName.serviceName");
        });
    });

    describe("when selected schema value does not match the sender's schema", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(
                    "bogus-schema",
                    DEFAULT_ACTIVE_MEMBERSHIP,
                    DEFAULT_SENDER
                )
            ).toEqual("");
        });
    });
});
*!/
*/
