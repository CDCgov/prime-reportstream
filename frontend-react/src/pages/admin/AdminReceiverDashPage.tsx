import { Helmet } from "react-helmet";

import HipaaNotice from "../../components/HipaaNotice";
import { AdminReceiverDashboard } from "../../components/Admin/AdminReceiverDashboard";

export function AdminReceiverDashPage() {
    return (
        <>
            <Helmet>
                <title>Admin Destination Dashboard</title>
            </Helmet>
            <section className="grid-container margin-top-0" />
            <AdminReceiverDashboard />
            <HipaaNotice />
        </>
    );
}
