import { fireEvent, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import {
    CustomerStatus,
    FileType,
    Format,
} from "../../utils/TemporarySettingsAPITypes";
import { RSSender } from "../../config/endpoints/settings";
import { fakeFile, mockSendValidFile } from "../../__mocks__/validation";
import { MemberType, MembershipSettings } from "../../utils/OrganizationUtils";
import { render } from "../../utils/Test/render";

import FileHandlerFileUploadStep, {
    getClientHeader,
} from "./FileHandlerFileUploadStep";

describe("FileHandlerFileUploadStep", () => {
    const DEFAULT_PROPS = {
        onFileChange: vi.fn(),
        onBack: vi.fn(),
        onSubmit: vi.fn(),
    };

    describe("when the Sender details have been loaded", () => {
        describe("when a CSV schema is chosen", () => {
            function setup() {
                render(
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
                render(
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
                render(
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
                render(
                    <FileHandlerFileUploadStep
                        {...DEFAULT_PROPS}
                        selectedSchemaOption={{
                            format: FileType.CSV,
                            title: "whatever",
                            value: "whatever",
                        }}
                        isSubmitting={true}
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

        describe("when a file is submitted", () => {
            async function setup() {
                render(
                    <FileHandlerFileUploadStep
                        {...DEFAULT_PROPS}
                        isValid
                        selectedSchemaOption={{
                            format: FileType.CSV,
                            title: "whatever",
                            value: "whatever",
                        }}
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

            test("it calls onSubmit", async () => {
                await setup();
                expect(DEFAULT_PROPS.onSubmit).toHaveBeenCalledWith(
                    mockSendValidFile,
                );
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
