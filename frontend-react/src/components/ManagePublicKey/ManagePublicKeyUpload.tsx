import React, { useRef } from "react";
import {
    Button,
    FileInput,
    FileInputRef,
    Form,
    FormGroup,
    Label,
} from "@trussworks/react-uswds";

export interface ManagePublicKeyFormProps {
    handleSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    handleFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    handleBack: () => void;
    fileInputResetValue: number;
    submitted: boolean;
    fileName: string;
}

const BASE_ACCEPT_VALUE = ".pem";

export const ManagePublicKeyUpload = ({
    handleSubmit,
    handleFileChange,
    handleBack,
    fileInputResetValue,
    submitted,
    fileName,
}: ManagePublicKeyFormProps) => {
    const isDisabled = fileName.length === 0;
    const fileInputRef = useRef<FileInputRef>(null);

    return (
        <Form
            name="managePublicKey"
            onSubmit={(e) => handleSubmit(e)}
            className="rs-full-width-form"
        >
            {!submitted && (
                <FormGroup className="margin-bottom-3">
                    <Label
                        className="font-sans-xs"
                        id="upload-pem-input-label"
                        htmlFor="upload-pem-input"
                    >
                        Upload public key
                        <br />
                        <span className="text-gray-50">
                            Make sure your file has a .pem extension and is
                            properly configured.
                        </span>
                    </Label>
                    <FileInput
                        key={fileInputResetValue}
                        id="upload-pem-input"
                        name="upload-pem-input"
                        aria-describedby="upload-pem-input-label"
                        data-testid="upload-pem-input"
                        onChange={(e) => handleFileChange(e)}
                        required
                        ref={fileInputRef}
                        accept={BASE_ACCEPT_VALUE}
                    />
                </FormGroup>
            )}
            <div className="grid-row">
                <div className="grid-col flex-1 display-flex flex-column flex-align-start">
                    <div className="grid-col flex-1 display-flex flex-row flex-align-start">
                        <Button onClick={handleBack} type="button" outline>
                            Back
                        </Button>
                        <Button disabled={isDisabled} type={"submit"}>
                            Submit
                        </Button>
                    </div>
                </div>
            </div>
        </Form>
    );
};
