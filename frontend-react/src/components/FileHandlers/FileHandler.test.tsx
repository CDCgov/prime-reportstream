import { fireEvent, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import {
    mockSendFileWithErrors,
    mockSendFileWithWarnings,
    mockSendValidFile,
} from "../../__mocks__/validation";
import * as useFileHandlerExports from "../../hooks/UseFileHandler";
import { FileHandlerState, INITIAL_STATE } from "../../hooks/UseFileHandler";
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

import FileHandler from "./FileHandler";

export const CSV_SCHEMA_SELECTED = {
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

export const VALID_CSV_FILE_SELECTED = {
    fileInputResetValue: 0,
    fileContent: "FAKE",
    fileName: "fake.csv",
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

export const INVALID_CSV_FILE_SELECTED = {
    fileInputResetValue: 0,
    fileContent: "INVALID",
    fileName: "invalid.csv",
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

export const WARNING_CSV_FILE_SELECTED = {
    fileInputResetValue: 0,
    fileContent: "WARNING",
    fileName: "warning.csv",
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

vi.mock("../../hooks/UseOrganizationSettings", async () => ({
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

export async function chooseSchema(schemaName: string) {
    expect(screen.getByText(/Select data model/)).toBeVisible();
    await userEvent.selectOptions(screen.getByRole("combobox"), [schemaName]);
}

export async function chooseFile(file: File) {
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
            dispatch: () => {},
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
        vi.spyOn(useWatersUploaderExports, "useWatersUploader").mockReturnValue(
            {
                isPending: false,
                error: null,
                mutateAsync: (() =>
                    Promise.resolve({})) as UseWatersUploaderSendFileMutation,
                ...result,
            } as any,
        );
    }

    async function schemaContinue() {
        await waitFor(async () => {
            await userEvent.click(screen.getByText("Continue"));
            expect(screen.getByTestId("form")).toBeInTheDocument();
        });
    }

    async function fileContinue() {
        await waitFor(async () => {
            const form = screen.getByTestId("form");
            // eslint-disable-next-line testing-library/no-wait-for-side-effects
            fireEvent.submit(form);
            await waitFor(() => expect(form).not.toBeInTheDocument());
        });
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

            render(<FileHandler />);
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
            mockUseFileHandler(VALID_CSV_FILE_SELECTED);
            mockUseSenderSchemaOptions({
                isLoading: false,
                data: STANDARD_SCHEMA_OPTIONS,
            });
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: () => Promise.resolve(mockSendValidFile),
            });

            render(<FileHandler />);
        }

        test("allows the user to upload a file and shows the success screen", async () => {
            setup();
            // Step 1: schema selection
            await schemaContinue();
            // Step 2: file upload
            await fileContinue();
            // Step 3: success
            await waitFor(() => {
                return screen.getByText(
                    "Your file is correctly formatted for ReportStream.",
                );
            });
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

            render(<FileHandler />);
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

            render(<FileHandler />);
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
