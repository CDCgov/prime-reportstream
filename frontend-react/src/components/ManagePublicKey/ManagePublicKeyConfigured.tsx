import React from "react";
import { Button, Grid, Icon } from "@trussworks/react-uswds";

type ManagePublicKeyUploadCompleteProps = {
    onUploadNewPublicKey: () => void;
};

export default function ManagePublicKeyConfigured({
    onUploadNewPublicKey,
}: ManagePublicKeyUploadCompleteProps) {
    return (
        <div className="manage-public-key-configured">
            <div className="margin-bottom-4">
                Your public key is already configured.
            </div>
            <Grid row>
                <Grid col="auto">
                    <Button onClick={onUploadNewPublicKey} type="button">
                        Upload new public key <Icon.Edit />
                    </Button>
                </Grid>
            </Grid>
        </div>
    );
}
