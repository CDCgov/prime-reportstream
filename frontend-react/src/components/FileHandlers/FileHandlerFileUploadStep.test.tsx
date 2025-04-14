import { fireEvent, screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { Suspense } from "react";

import FileHandlerFileUploadStep from "./FileHandlerFileUploadStep";
import { fakeFile, mockSendFileWithErrors, mockSendValidFile } from "../../__mocks__/validation";
import { sendersGenerator } from "../../__mockServers__/OrganizationMockServer";
import { RSSender } from "../../config/endpoints/settings";
import { UseSenderResourceHookResult } from "../../hooks/api/organizations/UseOrganizationSender/UseOrganizationSender";
import * as useSenderResourceExports from "../../hooks/api/organizations/UseOrganizationSender/UseOrganizationSender";
import * as useWatersUploaderExports from "../../hooks/api/UseWatersUploader/UseWatersUploader";
import useAppInsightsContext from "../../hooks/UseAppInsightsContext/UseAppInsightsContext";
import { INITIAL_STATE } from "../../hooks/UseFileHandler/UseFileHandler";
import { renderApp } from "../../utils/CustomRenderUtils";
import { MembershipSettings, MemberType } from "../../utils/OrganizationUtils";
import { getClientHeader } from "../../utils/SessionStorageTools";
import { CustomerStatus, FileType, Format } from "../../utils/TemporarySettingsAPITypes";

const { mockSessionContentReturnValue } = await vi.importMock<
    typeof import("../../contexts/Session/__mocks__/useSessionContext")
>("../../contexts/Session/useSessionContext");
const mockUseAppInsightsContext = vi.mocked(useAppInsightsContext);
const mockAppInsights = mockUseAppInsightsContext();

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

    function mockUseSenderResource(result: Partial<UseSenderResourceHookResult> = {}) {
        vi.spyOn(useSenderResourceExports, "default").mockReturnValue({
            isInitialLoading: false,
            isLoading: false,
            data: DEFAULT_SENDERS,
            ...result,
        } as UseSenderResourceHookResult);
    }

    describe("when the Sender details have been loaded", () => {
        beforeEach(() => {
            mockUseSenderResource({
                isInitialLoading: false,
                isLoading: false,
            });
            mockSessionContentReturnValue();
        });

        describe("when a CSV schema is chosen", () => {
            function setup() {
                renderApp(
                    <Suspense>
                        <FileHandlerFileUploadStep
                            {...DEFAULT_PROPS}
                            selectedSchemaOption={{
                                format: FileType.CSV,
                                title: "whatever",
                                value: "whatever",
                            }}
                        />
                    </Suspense>,
                );
            }

            test("renders the CSV-specific text", async () => {
                setup();
                await waitFor(() => expect(screen.getByText("Upload CSV file")).toBeVisible());
                expect(screen.getByText("Make sure your file has a .csv extension")).toBeVisible();
            });
        });

        describe("when an HL7 schema is chosen", () => {
            function setup() {
                renderApp(
                    <Suspense>
                        <FileHandlerFileUploadStep
                            {...DEFAULT_PROPS}
                            selectedSchemaOption={{
                                format: FileType.HL7,
                                title: "whatever",
                                value: "whatever",
                            }}
                        />
                    </Suspense>,
                );
            }

            test("renders the HL7-specific text", async () => {
                setup();
                await waitFor(() => expect(screen.getByText("Upload HL7 v2.5.1 file")).toBeVisible());
                expect(screen.getByText("Make sure your file has a .hl7 extension")).toBeVisible();
            });
        });

        describe("when a file is selected", () => {
            const onFileChangeSpy = vi.fn();
            async function setup() {
                renderApp(
                    <Suspense>
                        <FileHandlerFileUploadStep
                            {...DEFAULT_PROPS}
                            selectedSchemaOption={{
                                format: FileType.CSV,
                                title: "whatever",
                                value: "whatever",
                            }}
                            onFileChange={onFileChangeSpy}
                        />
                    </Suspense>,
                );

                await waitFor(async () => {
                    await userEvent.upload(screen.getByTestId("file-input-input"), fakeFile);
                    await new Promise((res) => setTimeout(res, 100));
                });
            }

            test("calls onFileChange with the file and content", async () => {
                await setup();
                expect(onFileChangeSpy).toHaveBeenCalledWith(fakeFile, "foo,bar\r\nbar,foo");
            });
        });

        describe("when a file is being submitted", () => {
            function setup() {
                vi.spyOn(useWatersUploaderExports, "default").mockReturnValue({
                    isPending: true,
                    error: null,
                    mutateAsync: () => Promise.resolve({}),
                } as any);

                renderApp(
                    <Suspense>
                        <FileHandlerFileUploadStep
                            {...DEFAULT_PROPS}
                            selectedSchemaOption={{
                                format: FileType.CSV,
                                title: "whatever",
                                value: "whatever",
                            }}
                        />
                    </Suspense>,
                );
            }

            test("renders the loading message", async () => {
                setup();
                await waitFor(() =>
                    expect(
                        screen.getByText(
                            "Checking your file for any errors that will prevent your data from being reported successfully...",
                        ),
                    ).toBeVisible(),
                );
            });
        });

        describe("when a valid file is submitted", () => {
            const onFileSubmitSuccessSpy = vi.fn();
            const onNextStepClickSpy = vi.fn();
            async function setup() {
                vi.spyOn(useWatersUploaderExports, "default").mockReturnValue({
                    isPending: false,
                    error: null,
                    mutateAsync: async () => await Promise.resolve(mockSendValidFile),
                } as any);

                renderApp(
                    <Suspense>
                        <FileHandlerFileUploadStep
                            {...DEFAULT_PROPS}
                            isValid
                            selectedSchemaOption={{
                                format: FileType.CSV,
                                title: "whatever",
                                value: "whatever",
                            }}
                            fileContent="whatever"
                            fileName="whatever.csv"
                            file={new File([new Blob(["whatever"])], "whatever.csv")}
                            onFileSubmitSuccess={onFileSubmitSuccessSpy}
                            onNextStepClick={onNextStepClickSpy}
                        />
                    </Suspense>,
                );

                const input = await screen.findByTestId("file-input-input");
                await userEvent.upload(input, fakeFile);
                await userEvent.click(screen.getByText("Submit"));
                const form = screen.getByTestId("form");
                await waitFor(() => {
                    // eslint-disable-next-line testing-library/no-wait-for-side-effects
                    fireEvent.submit(form);
                });
                await waitFor(() => expect(onFileSubmitSuccessSpy).toHaveBeenCalled());
            }

            afterEach(() => {
                vi.restoreAllMocks();
            });

            test("it calls onFileSubmitSuccess with the response", async () => {
                await setup();
                expect(onFileSubmitSuccessSpy).toHaveBeenCalledWith(mockSendValidFile);
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
                vi.spyOn(useWatersUploaderExports, "default").mockReturnValue({
                    isPending: false,
                    error: null,
                    mutateAsync: async () =>
                        // eslint-disable-next-line @typescript-eslint/prefer-promise-reject-errors
                        await Promise.reject({
                            data: mockSendFileWithErrors,
                        }),
                } as any);
                renderApp(
                    <Suspense>
                        <FileHandlerFileUploadStep
                            {...DEFAULT_PROPS}
                            isValid
                            selectedSchemaOption={{
                                format: FileType.CSV,
                                title: "whatever",
                                value: "whatever",
                            }}
                            fileContent="whatever"
                            fileName="whatever.csv"
                            file={new File([new Blob(["whatever"])], "whatever.csv")}
                            onFileSubmitError={onFileSubmitErrorSpy}
                        />
                    </Suspense>,
                );

                const input = await screen.findByTestId("file-input-input");
                await userEvent.upload(input, fakeFile);
                await userEvent.click(screen.getByText("Submit"));
                const form = screen.getByTestId("form");
                await waitFor(() => {
                    // eslint-disable-next-line testing-library/no-wait-for-side-effects
                    fireEvent.submit(form);
                });
                await waitFor(() => expect(onFileSubmitErrorSpy).toHaveBeenCalled());
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
        version: 0,
        createdAt: "",
        createdBy: "",
    };

    describe("when selectedSchemaName is falsy", () => {
        test("returns an empty string", () => {
            expect(getClientHeader(undefined, DEFAULT_ACTIVE_MEMBERSHIP, DEFAULT_SENDER)).toEqual("");
        });
    });

    describe("when activeMembership is falsy", () => {
        test("returns an empty string", () => {
            expect(getClientHeader(DEFAULT_SCHEMA_NAME, undefined, DEFAULT_SENDER)).toEqual("");
            expect(getClientHeader(DEFAULT_SCHEMA_NAME, null, DEFAULT_SENDER)).toEqual("");
        });
    });

    describe("when sender is falsy", () => {
        test("returns an empty string", () => {
            expect(getClientHeader(DEFAULT_SCHEMA_NAME, DEFAULT_ACTIVE_MEMBERSHIP, undefined)).toEqual("");
        });
    });

    describe("when activeMembership.parsedName is falsy", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(DEFAULT_SCHEMA_NAME, { ...DEFAULT_ACTIVE_MEMBERSHIP, parsedName: "" }, DEFAULT_SENDER),
            ).toEqual("");
        });
    });

    describe("when activeMembership.service is falsy", () => {
        test("returns an empty string", () => {
            expect(
                getClientHeader(DEFAULT_SCHEMA_NAME, { ...DEFAULT_ACTIVE_MEMBERSHIP, service: "" }, DEFAULT_SENDER),
            ).toEqual("");
        });
    });

    describe("when selected schema value matches sender's schema", () => {
        test("returns the client value from the organization's parsed name and service", () => {
            expect(getClientHeader(DEFAULT_SCHEMA_NAME, DEFAULT_ACTIVE_MEMBERSHIP, DEFAULT_SENDER)).toEqual(
                "orgName.serviceName",
            );
        });
    });

    describe("when selected schema value does not match the sender's schema", () => {
        test("returns an empty string", () => {
            expect(getClientHeader("bogus-schema", DEFAULT_ACTIVE_MEMBERSHIP, DEFAULT_SENDER)).toEqual("");
        });
    });
});
