import { Helmet } from "react-helmet";
import { NetworkErrorBoundary } from "rest-hooks";
import React, { Suspense } from "react";

import { useOrgName } from "../../hooks/UseOrgName";
import { ErrorPage } from "../error/ErrorPage";
import HipaaNotice from "../../components/HipaaNotice";
import Title from "../../components/Title";
import Spinner from "../../components/Spinner";

import SubmissionTable from "./SubmissionTable";

function Submissions() {
    const orgName: string = useOrgName();

    return (
        <>
            <Helmet>
                <title>Submissions | {process.env.REACT_APP_TITLE}</title>
            </Helmet>
            <section className="grid-container margin-top-5">
                <Title title="COVID-19" preTitle={orgName} />
            </section>
            <Suspense fallback={<Spinner />}>
                <NetworkErrorBoundary
                    fallbackComponent={(props) => (
                        <ErrorPage type="message" error={props.error} />
                    )}
                >
                    <SubmissionTable />
                </NetworkErrorBoundary>
            </Suspense>
            <HipaaNotice />
        </>
    );
}

export default Submissions;
