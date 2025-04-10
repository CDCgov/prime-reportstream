import site from "../../content/site.json";
import { StaticAlert, StaticAlertType } from "../StaticAlert";
import { USExtLink, USLink } from "../USLink";

export default function FileHandlerSuccessStep() {
    return (
        <div>
            <div className="margin-bottom-4">
                <StaticAlert
                    type={[StaticAlertType.Success]}
                    heading="File validated"
                    message="Your file is correctly formatted for ReportStream."
                />
            </div>
            <p className="font-size-18 margin-bottom-4">
                To continue your onboarding, email{" "}
                <USExtLink href={`mailto: ${site.orgs.RS.email}`}>{site.orgs.RS.email}</USExtLink> and let us know you
                have validated your file. Our team will respond soon to help you get set up in staging.
            </p>
            <p className="font-size-18 margin-0">
                Learn more about your next steps in{" "}
                <USLink href="/developer-resources/api-onboarding-guide#set-up-authentication-and-test-your-api-connection">
                    the onboarding process
                </USLink>
                .
            </p>
        </div>
    );
}
