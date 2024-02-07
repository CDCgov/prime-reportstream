import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";

import { AdminReceiverDashboard } from "../../components/Admin/AdminReceiverDashboard";
import HipaaNotice from "../../components/HipaaNotice";

export function AdminReceiverDashPage() {
    return (
        <GridContainer>
            <Helmet>
                <title>Receiver status dashboard - Admin</title>
            </Helmet>
            <article>
                <AdminReceiverDashboard />
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export default AdminReceiverDashPage;
