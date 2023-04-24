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

export interface ManagePublicKeyUploadProps {
    onPublicKeySubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onBack: () => void;
    hasBack: boolean;
    publicKey: boolean | File;
    file: File | null;
}

export default function ManagePublicKeyUpload({
    onPublicKeySubmit,
    onFileChange,
    onBack,
    hasBack,
    publicKey,
    file,
}: ManagePublicKeyUploadProps) {
    const isDisabled = !file;
    const fileInputRef = useRef<FileInputRef>(null);

    return (
        <>
            {publicKey && (
                <p className="font-sans-md">
                    Your public key is already configured.
                </p>
            )}
            <div data-testid="ManagePublicKeyUpload">
                <Form
                    name="public-key-upload"
                    onSubmit={(e) => {
                        e.preventDefault();
                        onPublicKeySubmit(e);
                    }}
                    className="rs-full-width-form"
                >
                    <FormGroup className="margin-bottom-3">
                        <Label
                            className="font-sans-xs"
                            id="upload-pem-input-label"
                            htmlFor="upload-pem-input"
                        >
                            <span className="display-block">
                                Upload public key
                            </span>
                            <span className="text-gray-50">
                                Make sure your file has a .pem extension and is
                                properly configured.
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
                            {hasBack && (
                                <Button onClick={onBack} type="button" outline>
                                    Back
                                </Button>
                            )}
                            <Button disabled={isDisabled} type="submit">
                                Submit
                            </Button>
                        </Grid>
                    </Grid>
                </Form>
            </div>
        </>
    );
}
