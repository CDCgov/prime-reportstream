import { fireEvent, screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";

import FileHandler from "./FileHandler";
import {
    mockSendFileWithErrors,
    mockSendFileWithWarnings,
    mockSendValidFile,
} from "../../__mocks__/validation";
import { RSSender } from "../../config/endpoints/settings";
import * as useWatersUploaderExports from "../../hooks/api/UseWatersUploader/UseWatersUploader";
import {
    UseWatersUploaderResult,
    UseWatersUploaderSendFileMutation,
} from "../../hooks/api/UseWatersUploader/UseWatersUploader";
import * as useFileHandlerExports from "../../hooks/UseFileHandler/UseFileHandler";
import {
    calculateRequestCompleteState,
    FileHandlerState,
    INITIAL_STATE,
} from "../../hooks/UseFileHandler/UseFileHandler";
import * as useSenderSchemaOptionsExports from "../../hooks/UseSenderSchemaOptions/UseSenderSchemaOptions";
import {
    STANDARD_SCHEMA_OPTIONS,
    UseSenderSchemaOptionsHookResult,
} from "../../hooks/UseSenderSchemaOptions/UseSenderSchemaOptions";
import { renderApp } from "../../utils/CustomRenderUtils";

const _CSV_SCHEMA_SELECTED = {
    fileInputResetValue: 0,
    fileContent: "",
    fileName: "",
    errors: [],
    destinations: "",
    reportItems: [],
    reportId: "",
    successTimestamp: "",
    cancellable: false,
    warnings: [],
    localError: "",
    overallStatus: "",
    selectedSchemaOption: STANDARD_SCHEMA_OPTIONS[0],
} satisfies FileHandlerState;

const VALID_CSV_FILE_SELECTED = {
    fileInputResetValue: 0,
    fileContent: "FAKE",
    fileName: "fake.csv",
    file: new File([new Blob(["FAKE"])], "fake.csv"),
    errors: [],
    destinations: "",
    reportItems: [],
    reportId: "",
    successTimestamp: "",
    cancellable: false,
    warnings: [],
    localError: "",
    overallStatus: "valid",
    selectedSchemaOption: STANDARD_SCHEMA_OPTIONS[0],
} satisfies FileHandlerState;

const INVALID_CSV_FILE_SELECTED = {
    fileInputResetValue: 0,
    fileContent: "INVALID",
    fileName: "invalid.csv",
    file: new File([new Blob(["INVALID"])], "invalid.csv"),
    errors: [{} as any],
    destinations: "",
    reportItems: [],
    reportId: "",
    successTimestamp: "",
    cancellable: false,
    warnings: [],
    localError: "",
    overallStatus: "",
    selectedSchemaOption: STANDARD_SCHEMA_OPTIONS[0],
} satisfies FileHandlerState;

const WARNING_CSV_FILE_SELECTED = {
    fileInputResetValue: 0,
    fileContent: "WARNING",
    fileName: "warning.csv",
    file: new File([new Blob(["WARNING"])], "warning.csv"),
    errors: [],
    destinations: "",
    reportItems: [],
    reportId: "",
    successTimestamp: "",
    cancellable: false,
    warnings: [{} as any],
    localError: "",
    overallStatus: "",
    selectedSchemaOption: STANDARD_SCHEMA_OPTIONS[0],
};

vi.mock(
    "../../hooks/api/organizations/UseOrganizationSettings/UseOrganizationSettings",
    () => ({
        default: () => {
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
    }),
);

vi.mock(
    "../../hooks/api/organizations/UseOrganizationSender/UseOrganizationSender",
    () => ({
        default: () => ({
            data: {
                name: "default",
                organizationName: "aegis",
            } satisfies Partial<RSSender>,
        }),
    }),
);

async function _chooseSchema(schemaName: string) {
    expect(screen.getByText(/Select data model/)).toBeVisible();
    await userEvent.selectOptions(screen.getByRole("combobox"), [schemaName]);
}

async function _chooseFile(file: File) {
    expect(screen.getByText("Drag file here or")).toBeVisible();
    await userEvent.upload(screen.getByTestId("file-input-input"), file);
    await screen.findByTestId("file-input-preview-image");
    await waitFor(() => expect(screen.getByText("Submit")).toBeEnabled());
}

describe("FileHandler", () => {
    function mockUseFileHandler(
        fileHandlerState: Partial<FileHandlerState> = {},
    ) {
        vi.spyOn(useFileHandlerExports, "default").mockReturnValue({
            state: {
                // TODO: any sensible defaults?
                ...fileHandlerState,
            } as FileHandlerState,
            dispatch: () => void 0,
        });
    }

    function mockUseSenderSchemaOptions(
        result: Partial<UseSenderSchemaOptionsHookResult> = {},
    ) {
        vi.spyOn(useSenderSchemaOptionsExports, "default").mockReturnValue({
            isLoading: false,
            data: STANDARD_SCHEMA_OPTIONS,
            ...result,
        } as any);
    }

    function mockUseWatersUploader(
        result: Partial<UseWatersUploaderResult> = {},
    ) {
        vi.spyOn(useWatersUploaderExports, "default").mockReturnValue({
            isPending: false,
            error: null,
            mutateAsync: (() =>
                Promise.resolve({})) as UseWatersUploaderSendFileMutation,
            ...result,
        } as any);
    }

    async function schemaContinue() {
        await waitFor(async () => {
            await userEvent.click(screen.getByText("Continue"));
            expect(screen.getByTestId("form")).toBeInTheDocument();
        });
    }

    async function fileContinue() {
        const form = screen.getByTestId("form");
        await waitFor(() => {
            // eslint-disable-next-line testing-library/no-wait-for-side-effects
            fireEvent.submit(form);
        });
        await waitFor(() => expect(form).not.toBeInTheDocument());
    }

    describe("by default", () => {
        function setup() {
            mockUseFileHandler(INITIAL_STATE);
            mockUseSenderSchemaOptions({
                isLoading: false,
                data: STANDARD_SCHEMA_OPTIONS,
            });
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: vi.fn(),
            });

            renderApp(<FileHandler />);
        }

        test("renders the prompt as expected", () => {
            setup();
            expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent(
                "ReportStream File Validator",
            );
            expect(screen.getByRole("heading", { level: 2 })).toHaveTextContent(
                "wow, cool organization",
            );
            expect(screen.getByText("Select data model")).toBeVisible();
        });
    });

    describe("when a valid CSV file is being submitted with no warnings or errors", () => {
        function setup() {
            mockUseFileHandler({
                ...VALID_CSV_FILE_SELECTED,
                ...calculateRequestCompleteState(VALID_CSV_FILE_SELECTED, {
                    response: mockSendValidFile,
                }),
            });
            mockUseSenderSchemaOptions({
                isLoading: false,
                data: STANDARD_SCHEMA_OPTIONS,
            });
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: async () => await Promise.resolve(mockSendValidFile),
            });

            renderApp(<FileHandler />);
        }

        test("allows the user to upload a file and shows the success screen", async () => {
            setup();
            // Step 1: schema selection
            await schemaContinue();
            // Step 2: file upload
            await fileContinue();
            // Step 3: success
            expect(
                await screen.findByText(
                    "Your file is correctly formatted for ReportStream.",
                ),
            ).toBeInTheDocument();
        });
    });

    describe("when a CSV file with warnings is being submitted", () => {
        function setup() {
            mockUseFileHandler(WARNING_CSV_FILE_SELECTED);
            mockUseSenderSchemaOptions({
                isLoading: false,
                data: STANDARD_SCHEMA_OPTIONS,
            });
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: () => Promise.resolve(mockSendFileWithWarnings),
            });

            renderApp(<FileHandler />);
        }

        test("allows the user to upload a file and shows the warnings screen", async () => {
            setup();
            // Step 1: schema selection
            await schemaContinue();

            // Step 2: file upload
            await fileContinue();

            // Step 3a: warnings
            expect(screen.getByText("Recommended edits found")).toBeVisible();

            // Step 3b: warnings modal
            expect(screen.getByText("Continue without changes")).toBeEnabled();
            await waitFor(async () => {
                await userEvent.click(screen.getByText(/^Continue$/));
                return screen.getByText(
                    "Your file is correctly formatted for ReportStream.",
                );
            });
        });
    });

    describe("when a CSV file with errors is being submitted", () => {
        function setup() {
            mockUseFileHandler(INVALID_CSV_FILE_SELECTED);
            mockUseSenderSchemaOptions({
                isLoading: false,
                data: STANDARD_SCHEMA_OPTIONS,
            });
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: () => Promise.resolve(mockSendFileWithErrors),
            });

            renderApp(<FileHandler />);
        }

        test("allows the user to upload and file and shows the error screen", async () => {
            setup();
            // Step 1: schema selection
            await schemaContinue();

            // Step 2: file upload
            await fileContinue();

            // Step 3: errors
            expect(
                screen.getByText("Resubmit with the required edits."),
            ).toBeVisible();
        });

        test("allows the user to test another file", async () => {
            setup();
            // Step 1: schema selection
            await schemaContinue();

            // Step 2: file upload
            await fileContinue();

            // Step 3: errors
            expect(
                screen.getByText("Resubmit with the required edits."),
            ).toBeVisible();
            expect(
                screen.queryByText("Continue without changes"),
            ).not.toBeInTheDocument();

            await waitFor(async () => {
                await userEvent.click(screen.getByText("Test another file"));

                // Step 2: file upload
                expect(screen.getByText("Drag file here or")).toBeVisible();
            });
        });
    });
});
