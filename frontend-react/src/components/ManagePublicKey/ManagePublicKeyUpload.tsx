import React, { useRef } from "react";
import {
    Button,
    FileInput,
    FileInputRef,
    Form,
    FormGroup,
    Grid,
    Label,
} from "@trussworks/react-uswds";

export interface ManagePublicKeyFormProps {
    onPublicKeySubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onBack: () => void;
    file: File | null;
}

export default function ManagePublicKeyUpload({
    onPublicKeySubmit,
    onFileChange,
    onBack,
    file,
}: ManagePublicKeyFormProps) {
    const isDisabled = !file;
    const fileInputRef = useRef<FileInputRef>(null);

    return (
        <Form
            name="manage-public-key"
            onSubmit={(e) => onPublicKeySubmit(e)}
            className="rs-full-width-form"
        >
            <FormGroup className="margin-bottom-3">
                <Label
                    className="font-sans-xs"
                    id="upload-pem-input-label"
                    htmlFor="upload-pem-input"
                >
                    <span className="display-block">Upload public key</span>
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
                    accept=".pem"
                />
            </FormGroup>
            <Grid row>
                <Grid col="auto">
                    <Button onClick={onBack} type="button" outline>
                        Back
                    </Button>
                    <Button disabled={isDisabled} type="submit">
                        Submit
                    </Button>
                </Grid>
            </Grid>
        </Form>
    );
}
