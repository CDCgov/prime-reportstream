import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import { MessageTracker } from "../../components/MessageTracker/MessageTracker";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

export function AdminMessageTracker() {
    return (
        <GridContainer>
            <Helmet>
                <title>Message Id Search</title>
            </Helmet>
            <article>
                <MessageTracker />
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export const AdminMessageTrackerPage = () =>
    withCatchAndSuspense(<AdminMessageTracker />);

export default AdminMessageTrackerPage;
