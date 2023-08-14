/* eslint-disable testing-library/no-unnecessary-act */

// Even though the linter complains about act(),
// the test will fail when submitting the form with
// fireEvent.submit() which requires that its wrapped
// in act()
import { act, fireEvent, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { renderApp } from "../../utils/CustomRenderUtils";
import {
    fakeFile,
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

export async function chooseSchema(schemaName: string) {
    expect(screen.getByText(/Select data model/)).toBeVisible();
    await userEvent.selectOptions(screen.getByRole("combobox"), [schemaName]);
}

export async function chooseFile(file: File) {
    expect(screen.getByText("Drag file here or")).toBeVisible();
    await userEvent.upload(screen.getByTestId("file-input-input"), file);
    await screen.findByTestId("file-input-preview-image");
}

describe("FileHandler", () => {
    function mockUseFileHandler(
        fileHandlerState: Partial<FileHandlerState> = {},
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
        result: Partial<UseSenderSchemaOptionsHookResult> = {},
    ) {
        jest.spyOn(useSenderSchemaOptionsExports, "default").mockReturnValue({
            isLoading: false,
            schemaOptions: STANDARD_SCHEMA_OPTIONS,
            ...result,
        });
    }

    function mockUseWatersUploader(
        result: Partial<UseWatersUploaderResult> = {},
    ) {
        jest.spyOn(
            useWatersUploaderExports,
            "useWatersUploader",
        ).mockReturnValue({
            isWorking: false,
            uploaderError: null,
            sendFile: (() =>
                Promise.resolve({})) as UseWatersUploaderSendFileMutation,
            ...result,
        });
    }

    describe("by default", () => {
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

        test("renders the prompt as expected", () => {
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
        beforeEach(() => {
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: () => Promise.resolve(mockSendValidFile),
            });

            renderApp(<FileHandler />);
        });

        test("allows the user to upload and file and shows the success screen", async () => {
            // Step 1: schema selection
            expect(screen.getByText("Continue")).toBeDisabled();
            await chooseSchema("upload-covid-19");
            await userEvent.click(screen.getByText("Continue"));

            // Step 2: file upload
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            // Step 3: success
            await waitFor(() => {
                return screen.getByText(
                    "Your file is correctly formatted for ReportStream.",
                );
            });
        });
    });

    describe("when a CSV file with warnings is being submitted", () => {
        beforeEach(() => {
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: () => Promise.resolve(mockSendFileWithWarnings),
            });

            renderApp(<FileHandler />);
        });

        test("allows the user to upload and file and shows the warnings screen", async () => {
            // Step 1: schema selection
            expect(screen.getByText("Continue")).toBeDisabled();
            await chooseSchema("upload-covid-19");
            await userEvent.click(screen.getByText("Continue"));

            // Step 2: file upload
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            // Step 3a: warnings
            expect(screen.getByText("Recommended edits found")).toBeVisible();

            // Step 3b: warnings modal
            expect(screen.getByText("Continue without changes")).toBeEnabled();
            await userEvent.click(screen.getByText(/^Continue$/));
            await waitFor(() => {
                return screen.getByText(
                    "Your file is correctly formatted for ReportStream.",
                );
            });
        });
    });

    describe("when a CSV file with errors is being submitted", () => {
        beforeEach(() => {
            mockUseWatersUploader({
                isWorking: false,
                uploaderError: null,
                sendFile: () => Promise.resolve(mockSendFileWithErrors),
            });

            renderApp(<FileHandler />);
        });

        test("allows the user to upload and file and shows the error screen", async () => {
            // Step 1: schema selection
            expect(screen.getByText("Continue")).toBeDisabled();
            await chooseSchema("upload-covid-19");
            await userEvent.click(screen.getByText("Continue"));

            // Step 2: file upload
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            // Step 3: errors
            expect(
                screen.getByText("Resubmit with the required edits."),
            ).toBeVisible();
        });

        test("allows the user to test another file", async () => {
            // Step 1: schema selection
            expect(screen.getByText("Continue")).toBeDisabled();
            await chooseSchema("upload-covid-19");
            await userEvent.click(screen.getByText("Continue"));

            // Step 2: file upload
            expect(screen.getByText("Submit")).toBeDisabled();
            await chooseFile(fakeFile);
            await act(async () => {
                await fireEvent.submit(screen.getByTestId("form"));
            });

            // Step 3: errors
            expect(
                screen.getByText("Resubmit with the required edits."),
            ).toBeVisible();
            expect(
                screen.queryByText("Continue without changes"),
            ).not.toBeInTheDocument();
            await userEvent.click(screen.getByText("Test another file"));

            // Step 2: file upload
            expect(screen.getByText("Drag file here or")).toBeVisible();
        });
    });
});

/* eslint-enable testing-library/no-unnecessary-act */
