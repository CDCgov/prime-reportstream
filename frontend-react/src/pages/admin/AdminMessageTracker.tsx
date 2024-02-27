import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";

import HipaaNotice from "../../components/HipaaNotice";
import { MessageTracker } from "../../components/MessageTracker/MessageTracker";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

export function AdminMessageTracker() {
    return (
        <GridContainer>
            <Helmet>
                <title>Message ID search - Admin</title>
                <meta
                    property="og:image"
                    content="/assets/img/open-graph-protocol/reportstream.png"
                />
                <meta property="og:image:alt" content="" />
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
