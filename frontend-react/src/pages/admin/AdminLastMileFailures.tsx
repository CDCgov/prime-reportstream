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
                <title>Last Mile Failures</title>
                <meta property="og:image" content="/assets/img/opengraph/reportstream.png" />
                <meta
                    property="og:image:alt"
                    content='"ReportStream" surrounded by an illustration of lines and boxes connected by colorful dots.'
                />
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
