import { fireEvent, screen } from "@testing-library/react";

import {
    renderWithFullAppContext,
    renderWithQueryProvider,
} from "../../utils/CustomRenderUtils";
import {
    OverallStatus,
    ResponseError,
    WatersResponse,
} from "../../config/endpoints/waters";
import * as useFileHandlerExports from "../../hooks/UseFileHandler";
import {
    FileHandlerState,
    FileType,
    INITIAL_STATE,
} from "../../hooks/UseFileHandler";
import { mockAppInsights } from "../../utils/__mocks__/ApplicationInsights";
import { EventName } from "../../utils/Analytics";
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
import * as useSessionContextExports from "../../contexts/SessionContext";
import { RSSessionContext } from "../../contexts/SessionContext";
import * as useSenderResourceExports from "../../hooks/UseSenderResource";
import { UseSenderResourceHookResult } from "../../hooks/UseSenderResource";
import { MembershipSettings, MemberType } from "../../hooks/UseOktaMemberships";
import { CustomerStatus, Format } from "../../utils/TemporarySettingsAPITypes";
import { RSSender } from "../../config/endpoints/settings";

import FileHandler, {
    getClientHeader,
    UPLOAD_PROMPT_DESCRIPTIONS,
} from "./FileHandler";

const mockSendFile: WatersResponse = {
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
            errorCode: "INVALID_MSG_PARSE_DATE",
        },
        {
            details: "",
            scope: "item",
            indices: [1, 2],
            trackingIds: ["371784", "612092"],
            field: "ORC-15 (order_test_date)",
            message:
                "Timestamp for order_test_date should be precise. Reformat to either the HL7 v2.4 TS or ISO 8601 standard format.",
            errorCode: "INVALID_MSG_PARSE_DATE",
        },
    ],
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

/*
  below is an example of a mocked File & mocked React file input change event we can use for future tests
  thx to https://evanteague.medium.com/creating-fake-test-events-with-typescript-jest-778018379d1e
*/

const contentString = "some file content";

// doesn't work out of the box as it somehow doesn't come with a .text method
const fakeFile = new File([new Blob([contentString])], "file.csv", {
    type: "hl7",
});
fakeFile.text = () => Promise.resolve(contentString);

const fakeFileList = {
    length: 1,
    item: () => fakeFile,
    [Symbol.iterator]: function* () {
        yield fakeFile;
    },
} as FileList;

const fileChangeEvent = {
    target: {
        files: fakeFileList,
    },
} as React.ChangeEvent<HTMLInputElement>;

describe("FileHandler", () => {
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

    function mockUseSenderResource(
        result: Partial<UseSenderResourceHookResult> = {}
    ) {
        jest.spyOn(
            useSenderResourceExports,
            "useSenderResource"
        ).mockReturnValue({
            ...result,
            senderDetail: {
                allowDuplicates: true,
                customerStatus: CustomerStatus.ACTIVE,
                format: FileType.CSV,
                name: "test",
                organizationName: "test",
                processingType: "sync",
                schemaName: "upload-covid-19",
                topic: "covid-19",
            },
            senderIsLoading: false,
            isInitialLoading: false,
        });
    }

    function mockUseSessionContext(result: Partial<RSSessionContext> = {}) {
        jest.spyOn(
            useSessionContextExports,
            "useSessionContext"
        ).mockReturnValue({
            oktaToken: {},
            activeMembership: {
                parsedName: "apple",
                memberType: MemberType.SENDER,
                service: "cantaloupe",
            },
            dispatch: () => {},
            initialized: true,
            isAdminStrictCheck: false,
            isUserAdmin: false,
            isUserSender: false,
            isUserReceiver: false,
            ...result,
        });
    }

    describe("when the sender schema options are loading", () => {
        beforeEach(() => {
            mockUseFileHandler(INITIAL_STATE);
            mockUseSenderSchemaOptions({ isLoading: true });
            mockUseWatersUploader();

            renderWithFullAppContext(<FileHandler />);
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

                renderWithQueryProvider(<FileHandler />);
            });

            test("renders as expected", () => {
                const headings = screen.getAllByRole("heading");
                expect(headings).toHaveLength(2);
                expect(headings[0]).toHaveTextContent(
                    "ReportStream File Validator"
                );
                expect(headings[1]).toHaveTextContent("wow, cool organization");

                expect(screen.getByText(/Drag file here/)).toBeVisible();
            });
        });

        describe("when a file is being submitted", () => {
            const selectedSchemaOption = STANDARD_SCHEMA_OPTIONS[0];
            let sendFileSpy: UseWatersUploaderSendFileMutation;

            beforeEach(async () => {
                sendFileSpy = jest.fn(() => Promise.resolve(mockSendFile));

                mockUseSessionContext();
                mockUseSenderResource();
                mockUseFileHandler({
                    ...INITIAL_STATE,
                    fileType: selectedSchemaOption.format,
                    fileName: "anything",
                    selectedSchemaOption,
                });
                mockUseWatersUploader({
                    isWorking: false,
                    uploaderError: null,
                    sendFile: sendFileSpy,
                });

                renderWithFullAppContext(<FileHandler />);

                fireEvent.change(
                    screen.getByTestId("file-input-input"),
                    fileChangeEvent
                );

                await screen.findByTestId("file-input-preview-image");

                fireEvent.click(screen.getByText("Validate"));
            });

            test("calls fetch with the correct parameters", () => {
                expect(sendFileSpy).toHaveBeenCalledWith({
                    client: "apple.cantaloupe",
                    contentType: undefined,
                    fileContent: "some file content",
                    fileName: "anything",
                    format: "CSV",
                    schema: "upload-covid-19",
                });
            });

            test("tracks the event", () => {
                expect(mockAppInsights.trackEvent).toHaveBeenCalledWith({
                    name: EventName.FILE_VALIDATOR,
                    properties: {
                        fileValidator: {
                            warningCount: 2,
                            errorCount: 0,
                            schema: "upload-covid-19",
                            fileType: "CSV",
                            sender: "aegis",
                        },
                    },
                });
            });
        });

        describe("after a file has been submitted", () => {
            beforeEach(() => {
                mockUseFileHandler(INITIAL_STATE);
                mockUseSenderSchemaOptions({
                    isLoading: false,
                    schemaOptions: STANDARD_SCHEMA_OPTIONS,
                });
                mockUseWatersUploader({
                    isWorking: true,
                    uploaderError: null,
                    sendFile: jest.fn(),
                });

                renderWithQueryProvider(<FileHandler />);
            });

            test("renders a loading indicator", () => {
                expect(
                    screen.queryByTestId("file-input-input")
                ).not.toBeInTheDocument();
                expect(
                    screen.getByLabelText("loading-indicator")
                ).toBeVisible();
            });
        });

        describe("when the submission has errors", () => {
            beforeEach(() => {
                mockUseFileHandler({
                    ...INITIAL_STATE,
                    errors: [{ message: "Error" } as ResponseError],
                });
                mockUseWatersUploader({
                    isWorking: false,
                    uploaderError: null,
                    sendFile: () => Promise.resolve({}),
                });
                renderWithFullAppContext(<FileHandler />);
            });

            test("renders error messages", () => {
                expect(screen.getByTestId("error-table")).toBeVisible();
                expect(
                    screen.queryByTestId("file-input-input")
                ).not.toBeInTheDocument();
                expect(
                    screen.getByText("Please review the errors below.")
                ).toBeVisible();
                expect(
                    screen.getByText("Your file has not passed validation")
                ).toBeVisible();
            });
        });

        describe("when the submission succeeded with a CSV file", () => {
            const selectedSchemaOption = STANDARD_SCHEMA_OPTIONS.find(
                (option) => option.format === FileType.CSV
            );

            beforeEach(() => {
                mockUseFileHandler({
                    ...INITIAL_STATE,
                    fileType: selectedSchemaOption?.format,
                    fileName: "anything",
                    selectedSchemaOption,
                    destinations: "1, 2",
                    reportId: "IDIDID",
                    successTimestamp: new Date(0).toString(),
                    overallStatus: OverallStatus.VALID,
                });
                mockUseWatersUploader({
                    isWorking: false,
                    uploaderError: null,
                    sendFile: () => Promise.resolve(mockSendFile),
                });

                renderWithFullAppContext(<FileHandler />);
            });

            test("does not render error messages", () => {
                expect(
                    screen.queryByTestId("error-table")
                ).not.toBeInTheDocument();
                expect(
                    screen.queryByText("Please review the errors below.")
                ).not.toBeInTheDocument();
                expect(
                    screen.queryByText("Your file has not passed validation")
                ).not.toBeInTheDocument();
            });

            test("renders a success message", () => {
                expect(
                    screen.queryByTestId("file-input-input")
                ).not.toBeInTheDocument();
                expect(
                    screen.getByText("The file meets the standard CSV schema.")
                ).toBeVisible();
            });
        });

        describe("when the submission succeeded with an HL7 file", () => {
            const selectedSchemaOption = STANDARD_SCHEMA_OPTIONS.find(
                (option) => option.format === FileType.HL7
            );

            beforeEach(() => {
                mockUseFileHandler({
                    ...INITIAL_STATE,
                    fileType: selectedSchemaOption?.format,
                    fileName: "anything",
                    selectedSchemaOption,
                    destinations: "1, 2",
                    reportId: "IDIDID",
                    successTimestamp: new Date(0).toString(),
                    overallStatus: OverallStatus.VALID,
                });
                mockUseWatersUploader({
                    isWorking: false,
                    uploaderError: null,
                    sendFile: () => Promise.resolve(mockSendFile),
                });

                renderWithFullAppContext(<FileHandler />);
            });

            test("does not render error messages", () => {
                expect(
                    screen.queryByTestId("error-table")
                ).not.toBeInTheDocument();
                expect(
                    screen.queryByText("Please review the errors below.")
                ).not.toBeInTheDocument();
                expect(
                    screen.queryByText("Your file has not passed validation")
                ).not.toBeInTheDocument();
            });

            test("renders a success message", () => {
                expect(
                    screen.queryByTestId("file-input-input")
                ).not.toBeInTheDocument();
                expect(
                    screen.getByText(
                        "The file meets the ReportStream standard HL7 v2.5.1 schema."
                    )
                ).toBeVisible();
            });
        });

        describe("when the submission has warnings", () => {
            beforeEach(() => {
                mockUseFileHandler({
                    ...INITIAL_STATE,
                    warnings: [{ message: "error" } as ResponseError],
                    reportId: "1",
                });
                mockUseWatersUploader({
                    isWorking: false,
                    uploaderError: null,
                    sendFile: () => Promise.resolve({}),
                });

                renderWithFullAppContext(<FileHandler />);
            });

            test("renders warnings", () => {
                expect(screen.getByTestId("error-table")).toBeVisible();
                expect(
                    screen.queryByTestId("file-input-input")
                ).not.toBeInTheDocument();
                expect(
                    screen.getByText(
                        "The following warnings were returned while processing your file. We recommend addressing warnings to enhance clarity."
                    )
                ).toBeVisible();
            });
        });

        describe("when selecting between schemas", () => {
            describe("when no schema is selected", () => {
                beforeEach(() => {
                    renderWithFullAppContext(<FileHandler />);
                });

                test("does not render a prompt", () => {
                    expect(
                        screen.queryByText(
                            UPLOAD_PROMPT_DESCRIPTIONS[FileType.CSV]
                        )
                    ).not.toBeInTheDocument();
                    expect(
                        screen.queryByText(
                            UPLOAD_PROMPT_DESCRIPTIONS[FileType.HL7]
                        )
                    ).not.toBeInTheDocument();
                });
            });

            describe("when a CSV schema is selected", () => {
                const selectedSchemaOption = STANDARD_SCHEMA_OPTIONS.find(
                    (option) => option.format === FileType.CSV
                );

                beforeEach(() => {
                    mockUseFileHandler({
                        ...INITIAL_STATE,
                        selectedSchemaOption,
                    });

                    renderWithFullAppContext(<FileHandler />);
                });

                test("only renders a prompt to upload a CSV file", () => {
                    expect(
                        screen.getByText(
                            UPLOAD_PROMPT_DESCRIPTIONS[FileType.CSV]
                        )
                    ).toBeVisible();
                    expect(
                        screen.queryByText(
                            UPLOAD_PROMPT_DESCRIPTIONS[FileType.HL7]
                        )
                    ).not.toBeInTheDocument();
                });
            });

            describe("when an HL7 schema is selected", () => {
                const selectedSchemaOption = STANDARD_SCHEMA_OPTIONS.find(
                    (option) => option.format === FileType.HL7
                );

                beforeEach(() => {
                    mockUseFileHandler({
                        ...INITIAL_STATE,
                        selectedSchemaOption,
                    });

                    renderWithFullAppContext(<FileHandler />);
                });

                test("only renders a prompt to upload an HL7 file", () => {
                    expect(
                        screen.queryByText(
                            UPLOAD_PROMPT_DESCRIPTIONS[FileType.CSV]
                        )
                    ).not.toBeInTheDocument();
                    expect(
                        screen.getByText(
                            UPLOAD_PROMPT_DESCRIPTIONS[FileType.HL7]
                        )
                    ).toBeVisible();
                });
            });
        });
    });
});

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
