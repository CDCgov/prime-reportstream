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
    onPublicKeySubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onBack: () => void;
    fileName: string;
}

const BASE_ACCEPT_VALUE = ".pem";

export const ManagePublicKeyUpload = ({
    onPublicKeySubmit,
    onFileChange,
    onBack,
    fileName,
}: ManagePublicKeyFormProps) => {
    const isDisabled = fileName.length === 0;
    const fileInputRef = useRef<FileInputRef>(null);

    return (
        <Form
            name="managePublicKey"
            onSubmit={(e) => onPublicKeySubmit(e)}
            className="rs-full-width-form"
        >
            <FormGroup className="margin-bottom-3">
                <Label
                    className="font-sans-xs"
                    id="upload-pem-input-label"
                    htmlFor="upload-pem-input"
                >
                    Upload public key
                    <br />
                    <span className="text-gray-50">
                        Make sure your file has a .pem extension and is properly
                        configured.
                    </span>
                </Label>
                <FileInput
                    id="upload-pem-input"
                    name="upload-pem-input"
                    aria-describedby="upload-pem-input-label"
                    data-testid="upload-pem-input"
                    onChange={(e) => onFileChange(e)}
                    required
                    ref={fileInputRef}
                    accept={BASE_ACCEPT_VALUE}
                />
            </FormGroup>
            <div className="grid-row">
                <div className="grid-col flex-1 display-flex flex-column flex-align-start">
                    <div className="grid-col flex-1 display-flex flex-row flex-align-start">
                        <Button onClick={onBack} type="button" outline>
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
