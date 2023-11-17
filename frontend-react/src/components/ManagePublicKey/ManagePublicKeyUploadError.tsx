import { Button, Grid } from "@trussworks/react-uswds";

import site from "../../content/site.json";
import { USExtLink } from "../USLink";
import Alert from "../../shared/Alert/Alert";

type ManagePublicKeyUploadCompleteProps = {
    onTryAgain: () => void;
};

export default function ManagePublicKeyUploadError({
    onTryAgain,
}: ManagePublicKeyUploadCompleteProps) {
    const heading = "Key could not be submitted";

    return (
        <div>
            <div className="margin-bottom-4">
                <Alert type="error" heading={heading} />
            </div>
            <div className="margin-bottom-4">
                Check that you uploaded an accepted file type and that your file
                is not blank.
            </div>
            <div className="margin-bottom-4">
                If you continue to get this error, email{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>
                    {site.orgs.RS.email}
                </USExtLink>{" "}
                for help.
            </div>
            <Grid row>
                <Grid col="auto">
                    <Button onClick={onTryAgain} type="button">
                        Try again
                    </Button>
                </Grid>
            </Grid>
        </div>
    );
}
