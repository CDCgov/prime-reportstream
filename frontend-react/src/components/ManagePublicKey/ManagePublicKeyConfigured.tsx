import site from "../../content/site.json";
import { USLink } from "../USLink";

export default function ManagePublicKeyConfigured() {
    return (
        <div className="manage-public-key-configured">
            <div className="margin-bottom-4">
                Your public key is already configured.{" "}
            </div>
            <div>
                {" "}
                <USLink href={site.forms.contactUs.url}>
                    Contact ReportStream
                </USLink>{" "}
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
