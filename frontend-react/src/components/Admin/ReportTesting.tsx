import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import useOrganizationSettings from "../../hooks/api/organizations/UseOrganizationSettings/UseOrganizationSettings";
import useReportTesting from "../../hooks/api/reports/UseReportTesting";
import AdminFetchAlert from "../alerts/AdminFetchAlert";
import Spinner from "../Spinner";

function ReportTesting() {
    const { data: organization } = useOrganizationSettings();
    const { testMessages, isDisabled, isLoading } = useReportTesting();
    if (isDisabled) {
        return <AdminFetchAlert />;
    }
    if (isLoading) return <Spinner />;

    console.log("testMessages = ", testMessages);
    return (
        <>
            <Helmet>
                <title>Message Testing - ReportStream</title>
            </Helmet>
            <GridContainer>
                <article>
                    <h1 className="margin-y-4">Message testing</h1>
                    {organization?.description && <h2 className="font-sans-lg">{organization.description}</h2>}
                    <p>
                        Test a message from the message bank or by entering a custom message. You can view test results
                        in this window while you are logged in. To save for later reference, you can open messages, test
                        results and output messages in separate tabs. 
                    </p>
                </article>
            </GridContainer>
        </>
    );
}

export default ReportTesting;
