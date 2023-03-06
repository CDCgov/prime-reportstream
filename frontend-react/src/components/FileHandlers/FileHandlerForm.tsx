import React, { useRef } from "react";
import {
    Button,
    Form,
    FormGroup,
    Label,
    FileInput,
    FileInputRef,
    Dropdown,
} from "@trussworks/react-uswds";

import { SchemaOption } from "../../senders/hooks/UseSenderSchemaOptions";
import { FileType } from "../../hooks/UseFileHandler";

import { FileHandlerSubmitButton } from "./FileHandlerButton";

export interface FileHandlerFormProps {
    handleSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    handleFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    resetState: () => void; // to handle resetting any parent state
    fileInputResetValue: number;
    submitted: boolean;
    cancellable: boolean;
    fileName: string;
    fileType?: FileType;
    formLabel: string;
    resetText: string;
    submitText: string;
    schemaOptions: SchemaOption[];
    selectedSchemaOption: SchemaOption | null;
    onSchemaChange: (schemaOption: SchemaOption | null) => void;
}

const BASE_ACCEPT_VALUE = [".csv", ".hl7"].join(",");

// form for submitting files to the api
// all state is controlled from above, and necessary elements and control functions are passed in
export const FileHandlerForm = ({
    handleSubmit,
    handleFileChange,
    resetState,
    fileInputResetValue,
    submitted,
    cancellable,
    fileName,
    fileType,
    formLabel,
    resetText,
    submitText,
    schemaOptions,
    selectedSchemaOption,
    onSchemaChange,
}: FileHandlerFormProps) => {
    const isDisabled = fileName.length === 0 || !selectedSchemaOption;
    const fileInputRef = useRef<FileInputRef>(null);
    const accept = selectedSchemaOption
        ? `.${selectedSchemaOption.format.toLowerCase()}`
        : BASE_ACCEPT_VALUE;

    return (
        <Form
            name="fileValidation"
            onSubmit={(e) => handleSubmit(e)}
            className="rs-full-width-form"
        >
            {!submitted && (
                <FormGroup className="margin-bottom-3">
                    <Label
                        className="font-sans-xs"
                        id="upload-csv-input-label"
                        htmlFor="upload-csv-input"
                    >
                        {formLabel}
                    </Label>
                    <FileInput
                        key={fileInputResetValue}
                        id="upload-csv-input"
                        name="upload-csv-input"
                        aria-describedby="upload-csv-input-label"
                        data-testid="upload-csv-input"
                        onChange={(e) => handleFileChange(e)}
                        required
                        ref={fileInputRef}
                        accept={accept}
                    />
                </FormGroup>
            )}
            <div className="grid-row">
                <div className="grid-col flex-1 display-flex flex-column flex-align-start">
                    {cancellable && (
                        <Button onClick={resetState} type="button" outline>
                            Cancel
                        </Button>
                    )}
                </div>
                <div className="grid-col flex-1" />
                <div className="grid-col flex-1 display-flex flex-column flex-align-end">
                    {!submitted && (
                        <Dropdown
                            id="upload-schema-select"
                            name="upload-schema-select"
                            value={selectedSchemaOption?.value || ""}
                            onChange={(e) => {
                                const option =
                                    schemaOptions.find(
                                        ({ value }) => value === e.target.value
                                    ) || null;

                                if (option?.format !== fileType) {
                                    fileInputRef.current?.clearFiles();
                                }

                                onSchemaChange(option);
                            }}
                        >
                            <option value="">Select a schema</option>
                            {schemaOptions.map(({ title, value }) => (
                                <option key={value} value={value}>
                                    {title}
                                </option>
                            ))}
                        </Dropdown>
                    )}
                    <FileHandlerSubmitButton
                        submitted={submitted}
                        disabled={isDisabled}
                        reset={resetState}
                        resetText={resetText}
                        submitText={submitText}
                    />
                </div>
            </div>
        </Form>
    );
};
