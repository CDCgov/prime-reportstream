import { screen } from "@testing-library/react";

import { renderApp } from "../../utils/CustomRenderUtils";
import * as UseSenderResourceExports from "../../hooks/UseSenderResource";
import { INITIAL_STATE } from "../../hooks/UseFileHandler";
import {
    CustomerStatus,
    FileType,
    Format,
} from "../../utils/TemporarySettingsAPITypes";
import { RSSender } from "../../config/endpoints/settings";
import { MembershipSettings, MemberType } from "../../hooks/UseOktaMemberships";

import FileHandlerFileUploadStep, {
    getClientHeader,
} from "./FileHandlerFileUploadStep";

describe("FileHandlerFileUploadStep", () => {
    const DEFAULT_PROPS = {
        ...INITIAL_STATE,
        onFileChange: jest.fn(),
        onFileSubmitError: jest.fn(),
        onFileSubmitSuccess: jest.fn(),
        onPrevStepClick: jest.fn(),
        onNextStepClick: jest.fn(),
    };

    describe("when the Sender details are still loading", () => {
        beforeEach(() => {
            jest.spyOn(
                UseSenderResourceExports,
                "useSenderResource"
            ).mockReturnValue({
                isInitialLoading: true,
                senderIsLoading: true,
            });

            renderApp(<FileHandlerFileUploadStep {...DEFAULT_PROPS} />);
        });

        test("renders the spinner", () => {
            expect(screen.getByTestId("rs-spinner")).toBeVisible();
        });
    });

    describe("when the Sender details have been loaded", () => {
        beforeEach(() => {
            jest.spyOn(
                UseSenderResourceExports,
                "useSenderResource"
            ).mockReturnValue({
                isInitialLoading: false,
                senderIsLoading: false,
                senderDetail: {
                    schemaName: "whatever",
                } as RSSender,
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
                    />
                );
            });

            test("renders the CSV-specific text", () => {
                expect(screen.getByText("Upload CSV file")).toBeVisible();
                expect(
                    screen.getByText(
                        "Make sure that your file has a .csv extension"
                    )
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
                    />
                );
            });

            test("renders the HL7-specific text", () => {
                expect(
                    screen.getByText("Upload HL7 v2.5.1 file")
                ).toBeVisible();
                expect(
                    screen.getByText(
                        "Make sure that your file has a .hl7 extension"
                    )
                ).toBeVisible();
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
