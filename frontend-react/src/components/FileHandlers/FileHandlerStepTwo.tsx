import React, { useRef } from "react";
import {
    Button,
    Form,
    FormGroup,
    Label,
    FileInput,
    FileInputRef,
} from "@trussworks/react-uswds";

import { SchemaOption } from "../../senders/hooks/UseSenderSchemaOptions";

import { FileHandlerSpinner, UPLOAD_PROMPT_DESCRIPTIONS } from "./FileHandler";

export interface FileHandlerFormProps {
    fileInputResetValue: number;
    fileName: string;
    handleFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    handlePrevFileHandlerStep: () => void;
    handleSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    isWorking: boolean;
    selectedSchemaOption: SchemaOption;
}

const BASE_ACCEPT_VALUE = [".csv", ".hl7"].join(",");

export const FileHandlerStepTwo = ({
    fileInputResetValue,
    fileName,
    handleFileChange,
    handlePrevFileHandlerStep,
    handleSubmit,
    isWorking,
    selectedSchemaOption,
}: FileHandlerFormProps) => {
    const fileInputRef = useRef<FileInputRef>(null);
    const accept = selectedSchemaOption
        ? `.${selectedSchemaOption.format.toLowerCase()}`
        : BASE_ACCEPT_VALUE;

    return (
        <>
            {isWorking ? (
                <FileHandlerSpinner
                    message={
                        <>
                            <p>
                                Checking your file for any errors that will
                                prevent
                            </p>
                            <p>your data from being reported successfully...</p>
                        </>
                    }
                />
            ) : (
                <Form
                    name="fileValidation"
                    onSubmit={(e) => {
                        handleSubmit(e);
                    }}
                    className="rs-full-width-form"
                >
                    <FormGroup className="margin-bottom-3">
                        <Label
                            className="font-sans-xs"
                            id="upload-csv-input-label"
                            htmlFor="upload-csv-input"
                        >
                            {
                                UPLOAD_PROMPT_DESCRIPTIONS[
                                    selectedSchemaOption.format
                                ]
                            }
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
                    <div className="grid-col display-flex">
                        <Button
                            className="usa-button flex-align-self-start height-5 margin-top-4 usa-button--outline"
                            type={"button"}
                            onClick={handlePrevFileHandlerStep}
                        >
                            Back
                        </Button>
                        <Button
                            disabled={!fileName.length}
                            className="usa-button flex-align-self-start height-5 margin-top-4"
                            type={"submit"}
                        >
                            Submit
                        </Button>
                    </div>
                </Form>
            )}
        </>
    );
};
