import { fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { INITIAL_STATE } from "../../hooks/UseFileHandler";
import {
    CustomerStatus,
    FileType,
    Format,
} from "../../utils/TemporarySettingsAPITypes";
import { RSSender } from "../../config/endpoints/settings";
import { MembershipSettings, MemberType } from "../../hooks/UseOktaMemberships";
import { UseSenderResourceHookResult } from "../../hooks/UseSenderResource";
import { renderApp } from "../../utils/CustomRenderUtils";
import * as useSenderResourceExports from "../../hooks/UseSenderResource";
import * as useWatersUploaderExports from "../../hooks/network/WatersHooks";
import * as analyticsExports from "../../utils/Analytics";
import {
    fakeFile,
    mockSendFileWithErrors,
    mockSendValidFile,
} from "../../__mocks__/validation";
import { sendersGenerator } from "../../__mocks__/OrganizationMockServer";

import FileHandlerFileUploadStep, {
    getClientHeader,
} from "./FileHandlerFileUploadStep";

jest.mock("../AlertNotifications", () => ({
    ...jest.requireActual("../AlertNotifications"),
    showError: jest.fn(),
}));

describe("FileHandlerFileUploadStep", () => {
    const DEFAULT_PROPS = {
        ...INITIAL_STATE,
        onFileChange: jest.fn(),
        onFileSubmitError: jest.fn(),
        onFileSubmitSuccess: jest.fn(),
        onPrevStepClick: jest.fn(),
        onNextStepClick: jest.fn(),
    };
    const DEFAULT_SENDERS: RSSender[] = sendersGenerator(2);

    function mockUseSenderResource(
        result: Partial<UseSenderResourceHookResult> = {},
    ) {
        jest.spyOn(useSenderResourceExports, "default").mockReturnValue({
            isInitialLoading: false,
            isLoading: false,
            data: DEFAULT_SENDERS,
            ...result,
        } as UseSenderResourceHookResult);
    }

    describe("when the Sender details are still loading", () => {
        beforeEach(() => {
            mockUseSenderResource({
                isInitialLoading: true,
                isLoading: true,
            });

            renderApp(<FileHandlerFileUploadStep {...DEFAULT_PROPS} />);
        });

        test("renders the spinner", () => {
            expect(screen.getByTestId("rs-spinner")).toBeVisible();
        });
    });

    describe("when the Sender details have been loaded", () => {
        beforeEach(() => {
            mockUseSenderResource({
                isInitialLoading: false,
                isLoading: false,
            });
        });

        describe("when a CSV schema is chosen", () => {
            beforeEach(() => {
                renderApp(
                    <FileHandlerFileUploadStep
                        {...DEFAULT_PROPS}
                        selectedSchemaOption={{
                            format: FileType.CSV,
                            title: "whatever",
                            value: "whatever",
                        }}
                    />,
                );
            });

            test("renders the CSV-specific text", () => {
                expect(screen.getByText("Upload CSV file")).toBeVisible();
                expect(
                    screen.getByText(
                        "Make sure your file has a .csv extension",
                    ),
                ).toBeVisible();
            });
        });

        describe("when an HL7 schema is chosen", () => {
            beforeEach(() => {
                renderApp(
                    <FileHandlerFileUploadStep
                        {...DEFAULT_PROPS}
                        selectedSchemaOption={{
                            format: FileType.HL7,
                            title: "whatever",
                            value: "whatever",
                        }}
                    />,
                );
            });

            test("renders the HL7-specific text", () => {
                expect(
                    screen.getByText("Upload HL7 v2.5.1 file"),
                ).toBeVisible();
                expect(
                    screen.getByText(
                        "Make sure your file has a .hl7 extension",
                    ),
                ).toBeVisible();
            });
        });

        describe("when a file is selected", () => {
            const onFileChangeSpy = jest.fn();

            beforeEach(async () => {
                renderApp(
                    <FileHandlerFileUploadStep
                        {...DEFAULT_PROPS}
                        selectedSchemaOption={{
                            format: FileType.CSV,
                            title: "whatever",
                            value: "whatever",
                        }}
                        onFileChange={onFileChangeSpy}
                    />,
                );

                await userEvent.upload(
                    screen.getByTestId("file-input-input"),
                    fakeFile,
                );
            });

            test("calls onFileChange with the file and content", async () => {
                expect(onFileChangeSpy).toHaveBeenCalledWith(
                    fakeFile,
                    "foo,bar\r\nbar,foo",
                );
            });
        });

        describe("when a file is being submitted", () => {
            beforeEach(async () => {
                jest.spyOn(
                    useWatersUploaderExports,
                    "useWatersUploader",
                ).mockReturnValue({
                    isWorking: true,
                    uploaderError: null,
                    sendFile: () => Promise.resolve({}),
                });

                renderApp(
                    <FileHandlerFileUploadStep
                        {...DEFAULT_PROPS}
                        selectedSchemaOption={{
                            format: FileType.CSV,
                            title: "whatever",
                            value: "whatever",
                        }}
                    />,
                );
            });

            test("renders the loading message", () => {
                expect(
                    screen.getByText(
                        "Checking your file for any errors that will prevent your data from being reported successfully...",
                    ),
                ).toBeVisible();
            });
        });

        describe("when a valid file is submitted", () => {
            const onFileSubmitSuccessSpy = jest.fn();
            const onNextStepClickSpy = jest.fn();

            beforeEach(async () => {
                jest.spyOn(
                    useWatersUploaderExports,
                    "useWatersUploader",
                ).mockReturnValue({
                    isWorking: false,
                    uploaderError: null,
                    sendFile: () => Promise.resolve(mockSendValidFile),
                });

                jest.spyOn(analyticsExports, "trackAppInsightEvent");

                renderApp(
                    <FileHandlerFileUploadStep
                        {...DEFAULT_PROPS}
                        isValid
                        selectedSchemaOption={{
                            format: FileType.CSV,
                            title: "whatever",
                            value: "whatever",
                        }}
                        fileContent="whatever"
                        onFileSubmitSuccess={onFileSubmitSuccessSpy}
                        onNextStepClick={onNextStepClickSpy}
                    />,
                );

                await userEvent.upload(
                    screen.getByTestId("file-input-input"),
                    fakeFile,
                );
                await userEvent.click(screen.getByText("Submit"));
                fireEvent.submit(screen.getByTestId("form"));
            });

            afterEach(() => {
                jest.restoreAllMocks();
            });

            test("it calls onFileSubmitSuccess with the response", () => {
                expect(onFileSubmitSuccessSpy).toHaveBeenCalledWith(
                    mockSendValidFile,
                );
            });

            test("it calls onNextStepClick", () => {
                expect(onNextStepClickSpy).toHaveBeenCalled();
            });

            test("it calls trackAppInsightEvent with event data", () => {
                expect(
                    analyticsExports.trackAppInsightEvent,
                ).toHaveBeenCalledWith("File Validator", {
                    fileValidator: {
                        errorCount: 0,
                        fileType: undefined,
                        overallStatus: "Valid",
                        schema: "whatever",
                        sender: undefined,
                        warningCount: 0,
                    },
                });
            });
        });

        describe("when a valid file is submitted", () => {
            const onFileSubmitErrorSpy = jest.fn();

            beforeEach(async () => {
                jest.spyOn(
                    useWatersUploaderExports,
                    "useWatersUploader",
                ).mockReturnValue({
                    isWorking: false,
                    uploaderError: null,
                    sendFile: () =>
                        Promise.reject({ data: mockSendFileWithErrors }),
                });

                jest.spyOn(analyticsExports, "trackAppInsightEvent");

                renderApp(
                    <FileHandlerFileUploadStep
                        {...DEFAULT_PROPS}
                        isValid
                        selectedSchemaOption={{
                            format: FileType.CSV,
                            title: "whatever",
                            value: "whatever",
                        }}
                        fileContent="whatever"
                        onFileSubmitError={onFileSubmitErrorSpy}
                    />,
                );

                await userEvent.upload(
                    screen.getByTestId("file-input-input"),
                    fakeFile,
                );
                await userEvent.click(screen.getByText("Submit"));
                fireEvent.submit(screen.getByTestId("form"));
            });

            afterEach(() => {
                jest.restoreAllMocks();
            });

            test("it calls onFileSubmitErrorSpy with the response", () => {
                expect(onFileSubmitErrorSpy).toHaveBeenCalled();
            });

            test("it calls trackAppInsightEvent with event data", () => {
                expect(
                    analyticsExports.trackAppInsightEvent,
                ).toHaveBeenCalledWith("File Validator", {
                    fileValidator: {
                        errorCount: 2,
                        fileType: undefined,
                        schema: "whatever",
                        sender: undefined,
                        warningCount: 0,
                    },
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
                    DEFAULT_SENDER,
                ),
            ).toEqual("");
        });
    });

    describe("when activeMembership is falsy", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(DEFAULT_SCHEMA_NAME, undefined, DEFAULT_SENDER),
            ).toEqual("");
            expect(
                getClientHeader(DEFAULT_SCHEMA_NAME, null, DEFAULT_SENDER),
            ).toEqual("");
        });
    });

    describe("when sender is falsy", () => {
        expect(
            getClientHeader(
                DEFAULT_SCHEMA_NAME,
                DEFAULT_ACTIVE_MEMBERSHIP,
                undefined,
            ),
        ).toEqual("");
    });

    describe("when activeMembership.parsedName is falsy", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(
                    DEFAULT_SCHEMA_NAME,
                    { ...DEFAULT_ACTIVE_MEMBERSHIP, parsedName: "" },
                    DEFAULT_SENDER,
                ),
            ).toEqual("");
        });
    });

    describe("when activeMembership.service is falsy", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(
                    DEFAULT_SCHEMA_NAME,
                    { ...DEFAULT_ACTIVE_MEMBERSHIP, service: "" },
                    DEFAULT_SENDER,
                ),
            ).toEqual("");
        });
    });

    describe("when selected schema value matches sender's schema", () => {
        test("returns the client value from the organization's parsed name and service", () => {
            expect(
                getClientHeader(
                    DEFAULT_SCHEMA_NAME,
                    DEFAULT_ACTIVE_MEMBERSHIP,
                    DEFAULT_SENDER,
                ),
            ).toEqual("orgName.serviceName");
        });
    });

    describe("when selected schema value does not match the sender's schema", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(
                    "bogus-schema",
                    DEFAULT_ACTIVE_MEMBERSHIP,
                    DEFAULT_SENDER,
                ),
            ).toEqual("");
        });
    });
});
