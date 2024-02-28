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
                    content="/assets/img/opengraph/reportstream.png"
                />
                <meta
                    property="og:image:alt"
                    content='"ReportStream" surrounded by an illustration of lines and boxes connected by colorful dots.'
                />
            </Helmet>
            <article>
                <AdminReceiverDashboard />
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export default AdminReceiverDashPage;
