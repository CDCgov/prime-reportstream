import Alert from "../../shared/Alert/Alert";
import { USLink } from "../USLink";

export default function ManagePublicKeyUploadSuccess() {
    const heading = "New public key received by ReportStream";

    return (
        <div>
            <div className="margin-bottom-4">
                <Alert type="success" heading={heading} />
            </div>
            <div className="margin-bottom-4">
                You can now submit data to ReportStream.
            </div>
            <p>
                Read more about{" "}
                <USLink href="/developer-resources/api/getting-started#set-up-authentication">
                    your next steps for setting up authentication
                </USLink>
                .
            </p>
        </div>
    );
}
