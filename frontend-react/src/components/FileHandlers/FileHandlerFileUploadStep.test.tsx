import { fireEvent, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { INITIAL_STATE } from "../../hooks/UseFileHandler";
import {
    CustomerStatus,
    FileType,
    Format,
} from "../../utils/TemporarySettingsAPITypes";
import { RSSender } from "../../config/endpoints/settings";
import { UseSenderResourceHookResult } from "../../hooks/UseSenderResource";
import { renderApp } from "../../utils/CustomRenderUtils";
import * as useSenderResourceExports from "../../hooks/UseSenderResource";
import * as useWatersUploaderExports from "../../hooks/network/WatersHooks";
import {
    fakeFile,
    mockSendFileWithErrors,
    mockSendValidFile,
} from "../../__mocks__/validation";
import { sendersGenerator } from "../../__mocks__/OrganizationMockServer";
import { mockSessionContentReturnValue } from "../../contexts/__mocks__/SessionContext";
import {
    mockAppInsightsContextReturnValue,
    mockAppInsights,
} from "../../contexts/__mocks__/AppInsightsContextOld";
import { MemberType, MembershipSettings } from "../../utils/OrganizationUtils";

import FileHandlerFileUploadStep, {
    getClientHeader,
} from "./FileHandlerFileUploadStep";

vi.mock("../AlertNotifications", async () => ({
    ...(await vi.importActual<typeof import("../AlertNotifications")>(
        "../AlertNotifications",
    )),
    showError: vi.fn(),
}));

describe("FileHandlerFileUploadStep", () => {
    const DEFAULT_PROPS = {
        ...INITIAL_STATE,
        onFileChange: vi.fn(),
        onFileSubmitError: vi.fn(),
        onFileSubmitSuccess: vi.fn(),
        onPrevStepClick: vi.fn(),
        onNextStepClick: vi.fn(),
    };
    const DEFAULT_SENDERS: RSSender[] = sendersGenerator(2);

    function mockUseSenderResource(
        result: Partial<UseSenderResourceHookResult> = {},
    ) {
        vi.spyOn(useSenderResourceExports, "default").mockReturnValue({
            isInitialLoading: false,
            isLoading: false,
            data: DEFAULT_SENDERS,
            ...result,
        } as UseSenderResourceHookResult);
    }

    describe("when the Sender details are still loading", () => {
        function setup() {
            mockUseSenderResource({
                isInitialLoading: true,
                isLoading: true,
            });
            mockSessionContentReturnValue();
            mockAppInsightsContextReturnValue();

            renderApp(<FileHandlerFileUploadStep {...DEFAULT_PROPS} />);
        }

        test("renders the spinner", () => {
            setup();
            expect(screen.getByTestId("rs-spinner")).toBeVisible();
        });
    });

    describe("when the Sender details have been loaded", () => {
        beforeEach(() => {
            mockUseSenderResource({
                isInitialLoading: false,
                isLoading: false,
            });
            mockSessionContentReturnValue();
            mockAppInsightsContextReturnValue();
        });

        describe("when a CSV schema is chosen", () => {
            function setup() {
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
            }

            test("renders the CSV-specific text", () => {
                setup();
                expect(screen.getByText("Upload CSV file")).toBeVisible();
                expect(
                    screen.getByText(
                        "Make sure your file has a .csv extension",
                    ),
                ).toBeVisible();
            });
        });

        describe("when an HL7 schema is chosen", () => {
            function setup() {
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
            }

            test("renders the HL7-specific text", () => {
                setup();
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
            const onFileChangeSpy = vi.fn();
            async function setup() {
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

                await waitFor(async () => {
                    await userEvent.upload(
                        screen.getByTestId("file-input-input"),
                        fakeFile,
                    );
                    await new Promise((res) => setTimeout(res, 100));
                });
            }

            test("calls onFileChange with the file and content", async () => {
                await setup();
                expect(onFileChangeSpy).toHaveBeenCalledWith(
                    fakeFile,
                    "foo,bar\r\nbar,foo",
                );
            });
        });

        describe("when a file is being submitted", () => {
            function setup() {
                vi.spyOn(
                    useWatersUploaderExports,
                    "useWatersUploader",
                ).mockReturnValue({
                    isPending: true,
                    error: null,
                    mutateAsync: () => Promise.resolve({}),
                } as any);

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
            }

            test("renders the loading message", () => {
                setup();
                expect(
                    screen.getByText(
                        "Checking your file for any errors that will prevent your data from being reported successfully...",
                    ),
                ).toBeVisible();
            });
        });

        describe("when a valid file is submitted", () => {
            const onFileSubmitSuccessSpy = vi.fn();
            const onNextStepClickSpy = vi.fn();
            async function setup() {
                vi.spyOn(
                    useWatersUploaderExports,
                    "useWatersUploader",
                ).mockReturnValue({
                    isPending: false,
                    error: null,
                    mutateAsync: () => Promise.resolve(mockSendValidFile),
                } as any);

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

                await waitFor(async () => {
                    await userEvent.upload(
                        screen.getByTestId("file-input-input"),
                        fakeFile,
                    );
                    await userEvent.click(screen.getByText("Submit"));
                    // eslint-disable-next-line testing-library/no-wait-for-side-effects
                    fireEvent.submit(screen.getByTestId("form"));
                    await new Promise((res) => setTimeout(res, 100));
                });
            }

            afterEach(() => {
                vi.restoreAllMocks();
            });

            test("it calls onFileSubmitSuccess with the response", async () => {
                await setup();
                expect(onFileSubmitSuccessSpy).toHaveBeenCalledWith(
                    mockSendValidFile,
                );
            });

            test("it calls onNextStepClick", async () => {
                await setup();
                expect(onNextStepClickSpy).toHaveBeenCalled();
            });

            test("it calls trackAppInsightEvent with event data", async () => {
                await setup();
                expect(mockAppInsights.trackEvent).toHaveBeenCalledWith({
                    name: "File Validator",
                    properties: {
                        fileValidator: {
                            errorCount: 0,
                            fileType: undefined,
                            overallStatus: "Valid",
                            schema: "whatever",
                            sender: undefined,
                            warningCount: 0,
                        },
                    },
                });
            });
        });

        describe("when an invalid file is submitted", () => {
            const onFileSubmitErrorSpy = vi.fn();
            async function setup() {
                vi.spyOn(
                    useWatersUploaderExports,
                    "useWatersUploader",
                ).mockReturnValue({
                    isPending: false,
                    error: null,
                    mutateAsync: () =>
                        Promise.reject({
                            data: mockSendFileWithErrors,
                        }),
                } as any);
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

                await waitFor(async () => {
                    await userEvent.upload(
                        screen.getByTestId("file-input-input"),
                        fakeFile,
                    );
                    await userEvent.click(screen.getByText("Submit"));
                    // eslint-disable-next-line testing-library/no-wait-for-side-effects
                    fireEvent.submit(screen.getByTestId("form"));
                    await new Promise((res) => setTimeout(res, 100));
                });
            }

            afterEach(() => {
                vi.restoreAllMocks();
            });

            test("it calls onFileSubmitErrorSpy with the response", async () => {
                await setup();
                expect(onFileSubmitErrorSpy).toHaveBeenCalled();
            });

            test("it calls trackAppInsightEvent with event data", async () => {
                await setup();
                expect(mockAppInsights.trackEvent).toHaveBeenCalledWith({
                    name: "File Validator",
                    properties: {
                        fileValidator: {
                            errorCount: 2,
                            fileType: undefined,
                            schema: "whatever",
                            sender: undefined,
                            warningCount: 0,
                        },
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
        test("returns an empty string", () => {
            expect(
                getClientHeader(
                    DEFAULT_SCHEMA_NAME,
                    DEFAULT_ACTIVE_MEMBERSHIP,
                    undefined,
                ),
            ).toEqual("");
        });
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
