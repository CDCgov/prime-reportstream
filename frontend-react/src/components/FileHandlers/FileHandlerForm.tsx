import React from "react";
import {
    Button,
    Form,
    FormGroup,
    Label,
    FileInput,
} from "@trussworks/react-uswds";

import Spinner from "../Spinner";
import { FileHandlerSubmitButton } from "./FileHandlerButton";

interface FileHandlerFormProps {
    handleSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    handleFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    resetState: () => void;
    fileInputResetValue: number;
    submitted: boolean;
    cancellable: boolean;
    isSubmitting: boolean;
    fileName: string;
}

// TODO: create passed functions within this component once reducer is in place
// Currently handling them above,  otherwise we'd wind up
// passing through too many state control functions here, with a reducer we can just pass dispatch
export const FileHandlerForm = ({
    handleSubmit,
    handleFileChange,
    fileInputResetValue,
    submitted,
    cancellable,
    isSubmitting,
    resetState,
    fileName,
}: FileHandlerFormProps) => {
    return (
        <Form onSubmit={(e) => handleSubmit(e)} className="rs-full-width-form">
            {!submitted && (
                <FormGroup className="margin-bottom-3">
                    <Label
                        className="font-sans-xs"
                        id="upload-csv-input-label"
                        htmlFor="upload-csv-input"
                    >
                        Select an HL7 or CSV formatted file to validate.
                    </Label>
                    <FileInput
                        key={fileInputResetValue}
                        id="upload-csv-input"
                        name="upload-csv-input"
                        aria-describedby="upload-csv-input-label"
                        onChange={(e) => handleFileChange(e)}
                        required
                    />
                </FormGroup>
            )}
            {isSubmitting && (
                <div className="grid-col flex-1 display-flex flex-column flex-align-center">
                    <div className="grid-row">
                        <Spinner />
                    </div>
                    <div className="grid-row">Processing file...</div>
                </div>
            )}
            <div className="grid-row">
                <div className="grid-col flex-1 display-flex flex-column flex-align-start">
                    {cancellable && !isSubmitting && (
                        <Button onClick={resetState} type="button" outline>
                            <span>Cancel</span>
                        </Button>
                    )}
                </div>
                <div className="grid-col flex-1" />
                <div className="grid-col flex-1 display-flex flex-column flex-align-end">
                    <FileHandlerSubmitButton
                        isSubmitting={isSubmitting}
                        submitted={submitted}
                        disabled={fileName.length === 0}
                        reset={resetState}
                        resetText="Validate another file"
                        submitText="Validate"
                    />
                </div>
            </div>
        </Form>
    );
};
