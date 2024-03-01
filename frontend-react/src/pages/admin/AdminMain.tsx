import { GridContainer } from "@trussworks/react-uswds";
import { Suspense } from "react";
import { Helmet } from "react-helmet-async";
import { NetworkErrorBoundary } from "rest-hooks";

import { OrgsTable } from "../../components/Admin/OrgsTable";
import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";

const fallbackPage = () => <ErrorPage type="page" />;
const fallbackMessage = () => <ErrorPage type="message" />;

export function AdminMainPage() {
    return (
        <NetworkErrorBoundary fallbackComponent={fallbackMessage}>
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
                    <NetworkErrorBoundary fallbackComponent={fallbackPage}>
                        <Suspense fallback={<Spinner />}>
                            <OrgsTable />
                        </Suspense>
                    </NetworkErrorBoundary>
                    <HipaaNotice />
                </article>
            </GridContainer>
        </NetworkErrorBoundary>
    );
}

export default AdminMainPage;
