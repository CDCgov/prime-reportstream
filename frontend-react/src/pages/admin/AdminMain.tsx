import { Suspense } from "react";
import { NetworkErrorBoundary } from "rest-hooks";
import { Helmet } from "react-helmet-async";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../components/HipaaNotice";
import Spinner from "../../components/Spinner";
import { ErrorPage } from "../error/ErrorPage";
import { OrgsTable } from "../../components/Admin/OrgsTable";
import { FeatureName } from "../../utils/FeatureName";

const fallbackPage = () => <ErrorPage type="page" />;
const fallbackMessage = () => <ErrorPage type="message" />;

export function AdminMainPage() {
    return (
        <NetworkErrorBoundary fallbackComponent={fallbackMessage}>
            <GridContainer>
                <article>
                    <Helmet>
                        <title>{FeatureName.ADMIN}</title>
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
