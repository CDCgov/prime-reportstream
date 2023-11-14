import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import { MessageTracker } from "../../components/MessageTracker/MessageTracker";

export function AdminMessageTrackerPage() {
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

export default AdminMessageTrackerPage;
