import { screen, fireEvent } from "@testing-library/react";

import { renderWithBase } from "../../utils/CustomRenderUtils";
import { STANDARD_SCHEMA_OPTIONS } from "../../senders/hooks/UseSenderSchemaOptions";
import { FileType } from "../../hooks/UseFileHandler";

import { FileHandlerForm, FileHandlerFormProps } from "./FileHandlerForm";

describe("FileHandlerForm", () => {
    const DEFAULT_PROPS: FileHandlerFormProps = {
        handleSubmit: () => {},
        handleFileChange: () => {},
        resetState: () => {},
        fileInputResetValue: 0,
        submitted: false,
        cancellable: true,
        fileName: "file.file",
        formLabel: "Label",
        resetText: "Reset",
        submitText: "Submit",
        schemaOptions: STANDARD_SCHEMA_OPTIONS,
        selectedSchemaOption: null,
        onSchemaChange: () => {},
    };

    function doRender(props: Partial<FileHandlerFormProps> = {}) {
        return renderWithBase(
            <FileHandlerForm {...DEFAULT_PROPS} {...props} />
        );
    }

    describe("when unsubmitted (default state)", () => {
        const submitSpy = jest.fn((e) => e.preventDefault()); // to prevent error message in console
        const fileChangeSpy = jest.fn();

        beforeEach(() => {
            doRender({
                submitted: false,
                handleSubmit: submitSpy,
                handleFileChange: fileChangeSpy,
            });
        });

        test("renders the input", () => {
            expect(screen.getByTestId("file-input-input")).toBeVisible();
        });

        test("renders the schema options", () => {
            STANDARD_SCHEMA_OPTIONS.forEach(({ title }) => {
                expect(screen.getByText(title)).toBeVisible();
            });
        });
    });

    describe("when submitted", () => {
        beforeEach(() => {
            doRender({
                submitted: true,
            });
        });

        test("renders a reset button instead of a submit button", () => {
            expect(screen.getByText("Reset")).toBeVisible();
            expect(screen.queryByText("Submit")).not.toBeInTheDocument();
        });

        test("omits the file input", () => {
            // this is to make sure that after the form is submitted that we remove the input
            // this test id is added by trussworks, so... hopefully they don't change it?
            const input = screen.queryByTestId("file-input-input");
            expect(input).not.toBeInTheDocument();
        });
    });

    describe("when cancellable", () => {
        const resetSpy = jest.fn();

        beforeEach(() => {
            doRender({
                cancellable: true,
                resetState: resetSpy,
            });
        });

        describe("when clicking cancel", () => {
            beforeEach(() => {
                const cancelButton = screen.getByText("Cancel");
                expect(cancelButton).toHaveAttribute("type", "button");

                fireEvent.click(cancelButton);
            });

            test("calls the resetState callback", () => {
                expect(resetSpy).toHaveBeenCalledTimes(1);
            });
        });
    });

    // NOTE: based on Trussworks data-testid values: https://github.com/trussworks/react-uswds/blob/main/src/components/forms/FileInput/FileInput.tsx#L196
    describe("with accept values", () => {
        describe("when a schema option is selected", () => {
            beforeEach(() => {
                doRender({
                    selectedSchemaOption: {
                        title: "whatever-csv",
                        value: "whatever-csv",
                        format: FileType.CSV,
                    },
                });
            });

            test("only allows files from the selected format", () => {
                expect(screen.getByTestId("file-input-input")).toHaveAttribute(
                    "accept",
                    ".csv"
                );
            });
        });

        describe("when a schema option is not selected", () => {
            beforeEach(() => {
                doRender({
                    selectedSchemaOption: null,
                });
            });

            test("allows .csv and .hl7 files", () => {
                expect(screen.getByTestId("file-input-input")).toHaveAttribute(
                    "accept",
                    ".csv,.hl7"
                );
            });
        });
    });
});
