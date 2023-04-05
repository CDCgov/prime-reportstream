import React from "react";
import { Button, Grid } from "@trussworks/react-uswds";

type ManagePublicKeyUploadCompleteProps = {
    onUploadNewPublicKey: () => void;
};

export default function ManagePublicKeyConfigured({
    onUploadNewPublicKey,
}: ManagePublicKeyUploadCompleteProps) {
    return (
        <div>
            <h1 className="margin-top-0 margin-bottom-5">Manage Public Key</h1>
            <p className="font-sans-md">
                Your public key is already configured.
            </p>
            <Grid row>
                <Grid col="auto">
                    <Button
                        onClick={onUploadNewPublicKey}
                        type="button"
                        outline
                    >
                        Try Again
                    </Button>
                </Grid>
            </Grid>
        </div>
    );
}
