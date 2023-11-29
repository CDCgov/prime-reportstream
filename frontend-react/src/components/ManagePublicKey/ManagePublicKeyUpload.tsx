import React, { useCallback, useRef } from "react";
import {
    Button,
    FileInput,
    FileInputRef,
    Form,
    FormGroup,
    Grid,
    Label,
} from "@trussworks/react-uswds";

import useAsyncSafeCallback from "../../hooks/UseAsyncSafeCallback";

export interface ManagePublicKeyUploadProps {
    onSubmit: (e: React.FormEvent<HTMLFormElement>) => Promise<void>;
    onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onBack: () => void;
    publicKey: boolean | File;
    file: File | null;
}

export default function ManagePublicKeyUpload({
    onSubmit,
    onFileChange,
    onBack,
    publicKey,
    file,
}: ManagePublicKeyUploadProps) {
    const isDisabled = !file;
    const fileInputRef = useRef<FileInputRef>(null);
    const _handleSubmit = useCallback(
        async (e: React.FormEvent<HTMLFormElement>) => {
            e.preventDefault();
            await onSubmit(e);
        },
        [onSubmit],
    );
    const handleSubmit = useAsyncSafeCallback(_handleSubmit);

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
                    onSubmit={handleSubmit}
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
                            onChange={onFileChange}
                            required
                            ref={fileInputRef}
                            accept=".pem"
                            crossOrigin={undefined}
                        />
                    </FormGroup>
                    <Grid row>
                        <Grid col="auto">
                            {onBack && (
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
