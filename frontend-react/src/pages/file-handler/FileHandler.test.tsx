import { fireEvent, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import {
    mockSendFileWithErrors,
    mockSendFileWithWarnings,
} from "../../__mocks__/validation";
import * as useFileHandlerExports from "../../hooks/UseFileHandler";
import { FileHandlerState, INITIAL_STATE } from "../../hooks/UseFileHandler";
import * as useSenderSchemaOptionsExports from "../../senders/hooks/UseSenderSchemaOptions";
import {
    STANDARD_SCHEMA_OPTIONS,
    UseSenderSchemaOptionsHookResult,
} from "../../senders/hooks/UseSenderSchemaOptions";
import { render } from "../../utils/Test/render";
import { FileType } from "../../utils/TemporarySettingsAPITypes";

import FileHandlerBase from "./FileHandler";

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
    fileType: FileType.CSV,
} satisfies FileHandlerState;

export const EMPTY_CSV_FILE_SELECTED = {
    fileInputResetValue: 0,
    fileContent: "",
    fileName: "empty.csv",
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
    fileType: FileType.CSV,
};

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
    fileType: FileType.CSV,
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
    fileType: FileType.CSV,
};

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
    const mockOnError = vi.fn();
    const mockOnSubmit = vi.fn();
    const mockOnSuccess = vi.fn();
    const subHeader = "wow, cool organization";

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

    async function schemaContinue() {
        await waitFor(async () => {
            await userEvent.click(screen.getByText("Continue"));
            expect(screen.getByTestId("form")).toBeInTheDocument();
        });
    }

    async function fileContinue() {
        const form = screen.getByTestId("form");
        await waitFor(async () => {
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

            render(
                <FileHandlerBase
                    client=""
                    onError={mockOnError}
                    onSuccess={mockOnSuccess}
                    onSubmit={mockOnSubmit}
                    contactEmail=""
                    subHeader={subHeader}
                />,
            );
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
            mockOnSubmit.mockResolvedValue({});

            render(
                <FileHandlerBase
                    client=""
                    onError={mockOnError}
                    onSuccess={mockOnSuccess}
                    onSubmit={mockOnSubmit}
                    contactEmail=""
                    subHeader={subHeader}
                />,
            );
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

        test("it calls onSuccess with event data", async () => {
            setup();
            await schemaContinue();
            await fileContinue();
            expect(mockOnSuccess).toHaveBeenCalledWith({});
        });
    });

    describe("when a CSV file with warnings is being submitted", () => {
        function setup() {
            mockUseFileHandler(WARNING_CSV_FILE_SELECTED);
            mockUseSenderSchemaOptions({
                isLoading: false,
                data: STANDARD_SCHEMA_OPTIONS,
            });
            mockOnSubmit.mockResolvedValue(mockSendFileWithWarnings);

            render(
                <FileHandlerBase
                    client=""
                    onError={mockOnError}
                    onSuccess={mockOnSuccess}
                    onSubmit={mockOnSubmit}
                    contactEmail=""
                    subHeader={subHeader}
                />,
            );
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

        test("it calls onSuccess with event data", async () => {
            setup();
            await schemaContinue();
            await fileContinue();
            expect(mockOnSuccess).toHaveBeenCalledWith(
                mockSendFileWithWarnings,
            );
        });
    });

    describe("when a CSV file with errors is being submitted", () => {
        function setup() {
            mockUseFileHandler(INVALID_CSV_FILE_SELECTED);
            mockUseSenderSchemaOptions({
                isLoading: false,
                data: STANDARD_SCHEMA_OPTIONS,
            });
            mockOnSubmit.mockResolvedValue(mockSendFileWithErrors);

            render(
                <FileHandlerBase
                    client=""
                    onError={mockOnError}
                    onSuccess={mockOnSuccess}
                    onSubmit={mockOnSubmit}
                    contactEmail=""
                    subHeader={subHeader}
                />,
            );
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

        test("it calls onSuccess with event data", async () => {
            setup();
            await schemaContinue();
            await fileContinue();
            expect(mockOnSuccess).toHaveBeenCalledWith(mockSendFileWithErrors);
        });
    });

    describe("when an unexpected error occurs", () => {
        function setup() {
            mockUseFileHandler(EMPTY_CSV_FILE_SELECTED);
            mockUseSenderSchemaOptions({
                isLoading: false,
                data: STANDARD_SCHEMA_OPTIONS,
            });
            mockOnSubmit.mockImplementation(() => {
                throw new Error("Unknown error");
            });

            render(
                <FileHandlerBase
                    client=""
                    onError={mockOnError}
                    onSuccess={mockOnSuccess}
                    onSubmit={mockOnSubmit}
                    contactEmail=""
                    subHeader={subHeader}
                />,
            );
        }

        test("it calls onError with event data", async () => {
            setup();
            await schemaContinue();
            const form = screen.getByTestId("form");
            await waitFor(async () => {
                fireEvent.submit(form);
            });
            expect(mockOnError).toHaveBeenCalledWith(
                new Error("No file contents to validate"),
                {
                    fileType: EMPTY_CSV_FILE_SELECTED.fileType,
                    schema: EMPTY_CSV_FILE_SELECTED.selectedSchemaOption.value,
                },
            );
        });
    });
});
