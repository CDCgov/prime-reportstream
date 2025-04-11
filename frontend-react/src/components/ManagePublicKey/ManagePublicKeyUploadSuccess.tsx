import site from "../../content/site.json";
import { StaticAlert, StaticAlertType } from "../StaticAlert";
import { USLink } from "../USLink";

export default function ManagePublicKeyUploadSuccess() {
    const heading = "New public key received by ReportStream";

    return (
        <div>
            <div className="margin-bottom-4">
                <StaticAlert type={[StaticAlertType.Success]} heading={heading} />
            </div>
            <div className="margin-bottom-4">You can now submit data to ReportStream.</div>
            <p>
                Read more about{" "}
                <USLink
                    href={`${site.developerResources.apiOnboardingGuide.url}#set-up-authentication-and-test-your-api-connection`}
                >
                    your next steps for setting up authentication
                </USLink>
                .
            </p>
        </div>
    );
}
