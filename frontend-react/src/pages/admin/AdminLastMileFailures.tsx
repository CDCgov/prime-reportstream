import React, { Suspense } from "react";
import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import Spinner from "../../components/Spinner";
import HipaaNotice from "../../components/HipaaNotice";
import { AdminLastMileFailuresTable } from "../../components/Admin/AdminLastMileFailuresTable";
import { FeatureName } from "../../utils/FeatureName";

export function AdminLastMileFailuresPage() {
    return (
        <GridContainer>
            <Helmet>
                <title>{FeatureName.ADMIN}</title>
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
