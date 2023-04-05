import React, { useEffect, useRef } from "react";
import {
    Button,
    FileInput,
    FileInputRef,
    Form,
    FormGroup,
    Grid,
    Label,
} from "@trussworks/react-uswds";

import { useOrganizationPublicKeys } from "../../hooks/UseOrganizationPublicKeys";
import { useSessionContext } from "../../contexts/SessionContext";
import { ApiKey } from "../../config/endpoints/settings";

export interface ManagePublicKeyUploadProps {
    onFetchPublicKey: (hasPublicKey: boolean) => void;
    onPublicKeySubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onBack: () => void;
    showBack: boolean;
    file: File | null;
    sender: string;
}

export default function ManagePublicKeyUpload({
    onFetchPublicKey,
    onPublicKeySubmit,
    onFileChange,
    onBack,
    showBack,
    file,
    sender,
}: ManagePublicKeyUploadProps) {
    const isDisabled = !file;
    const fileInputRef = useRef<FileInputRef>(null);
    const { activeMembership } = useSessionContext();
    const { data } = useOrganizationPublicKeys();
    const kid = `${activeMembership?.parsedName}.${sender}`;

    useEffect(() => {
        if (data?.keys.length) {
            // check if kid already exists for the selected org.sender
            for (const apiKeys of data.keys) {
                if (apiKeys.keys.some((k: ApiKey) => k.kid === kid)) {
                    onFetchPublicKey(true);
                }
            }
        }
    }, [data, onFetchPublicKey]);

    return (
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
                        <span className="display-block">Upload public key</span>
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
                        {showBack && (
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
    );
}
