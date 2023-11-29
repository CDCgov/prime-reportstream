import { Button, Grid } from "@trussworks/react-uswds";
import { useErrorBoundary } from "react-error-boundary";

import { USExtLink } from "../USLink";
import Alert from "../../shared/Alert/Alert";
import { useSessionContext } from "../../contexts/Session";

export interface ManagePublicKeyUploadErrorBaseProps {
    email: string;
}

export function ManagePublicKeyUploadErrorBase({
    email,
}: ManagePublicKeyUploadErrorBaseProps) {
    const { resetBoundary } = useErrorBoundary();
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
                <USExtLink href={`mailto: ${email}`}>{email}</USExtLink> for
                help.
            </div>
            <Grid row>
                <Grid col="auto">
                    <Button onClick={resetBoundary} type="button">
                        Try again
                    </Button>
                </Grid>
            </Grid>
        </div>
    );
}

export interface ManagePublicKeyPageProps {}

export default function ManagePublicKeyUploadError(
    props: ManagePublicKeyPageProps,
) {
    const { site } = useSessionContext();
    return (
        <ManagePublicKeyUploadErrorBase {...props} email={site.orgs.RS.email} />
    );
}
