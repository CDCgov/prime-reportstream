import { GridContainer } from "@trussworks/react-uswds";
import { Suspense } from "react";
import { Helmet } from "react-helmet-async";

import { OrgsTable } from "../../components/Admin/OrgsTable";
import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";

export function AdminMainPage() {
    return (
        <GridContainer>
            <article>
                <Helmet>
                    <title>Organizations - Admin</title>
                    <meta
                        property="og:image"
                        content="/assets/img/opengraph/reportstream.png"
                    />
                    <meta
                        property="og:image:alt"
                        content='"ReportStream" surrounded by an illustration of lines and boxes connected by colorful dots.'
                    />
                </Helmet>
                <Suspense fallback={<Spinner />}>
                    <OrgsTable />
                </Suspense>
                <HipaaNotice />
            </article>
        </GridContainer>
    );
}

export default AdminMainPage;
