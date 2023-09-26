import React from "react";

import { Link } from "../../shared/Link/Link";

export default function ManagePublicKeyConfigured() {
    return (
        <div className="manage-public-key-configured">
            <div className="margin-bottom-4">
                Your public key is already configured.{" "}
            </div>
            <div>
                {" "}
                <Link href="/support/service-request">
                    Contact ReportStream
                </Link>{" "}
                to upload a new public key.
            </div>
            {/*<Grid row>
                <Grid col="auto">
                    <Button onClick={onUploadNewPublicKey} type="button">
                        Upload new public key <Icon.Edit />
                    </Button>
                </Grid>
            </Grid>*/}
        </div>
    );
}
