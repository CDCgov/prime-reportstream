import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";

import { AdminReceiverDashboard } from "../../components/Admin/AdminReceiverDashboard";
import HipaaNotice from "../../components/HipaaNotice";

export function AdminReceiverDashPage() {
    return (
        <GridContainer>
            <Helmet>
                <title>Receiver status dashboard - Admin</title>
                <meta
                    property="og:image"
                    content="/assets/img/open-graph-protocol/reportstream.png"
                />
                <meta property="og:image:alt" content="" />
            </Helmet>
            <article>
                <AdminReceiverDashboard />
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export default AdminReceiverDashPage;
