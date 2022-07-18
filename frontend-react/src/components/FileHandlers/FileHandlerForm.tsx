import React from "react";
import {
    Button,
    Form,
    FormGroup,
    Label,
    FileInput,
} from "@trussworks/react-uswds";

import { FileHandlerSubmitButton } from "./FileHandlerButton";

// NOTE: many of this props will go away / change after we refactor FileHandler to use a reducer
interface FileHandlerFormProps {
    handleSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    handleFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    resetState: () => void;
    fileInputResetValue: number;
    submitted: boolean;
    cancellable: boolean;
    fileName: string;
    formLabel: string;
    resetText: string;
    submitText: string;
}

// TODO: create passed functions within this component once reducer is in place
// Currently handling them above,  otherwise we'd wind up
// passing through too many state control functions here, with a reducer we can just pass dispatch
export const FileHandlerForm = ({
    handleSubmit,
    handleFileChange,
    resetState,
    fileInputResetValue,
    submitted,
    cancellable,
    fileName,
    formLabel,
    resetText,
    submitText,
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
                        {formLabel}
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
            <div className="grid-row">
                <div className="grid-col flex-1 display-flex flex-column flex-align-start">
                    {cancellable && (
                        <Button onClick={resetState} type="button" outline>
                            <span>Cancel</span>
                        </Button>
                    )}
                </div>
                <div className="grid-col flex-1" />
                <div className="grid-col flex-1 display-flex flex-column flex-align-end">
                    <FileHandlerSubmitButton
                        submitted={submitted}
                        disabled={fileName.length === 0}
                        reset={resetState}
                        resetText={resetText}
                        submitText={submitText}
                    />
                </div>
            </div>
        </Form>
    );
};
