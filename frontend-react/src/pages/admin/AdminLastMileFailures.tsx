import { GridContainer } from "@trussworks/react-uswds";
import { Suspense } from "react";
import { Helmet } from "react-helmet-async";

import { AdminLastMileFailuresTable } from "../../components/Admin/AdminLastMileFailuresTable";
import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";

export function AdminLastMileFailuresPage() {
    return (
        <GridContainer>
            <Helmet>
                <title>Last mile failures</title>
            </Helmet>
            <article className="margin-bottom-5">
                <h3 className="margin-bottom-0">
                    <Suspense fallback={<Spinner />} />
                </h3>
                <AdminLastMileFailuresTable />
            </article>
            <HipaaNotice />
        </GridContainer>
    );
}

export default AdminLastMileFailuresPage;
