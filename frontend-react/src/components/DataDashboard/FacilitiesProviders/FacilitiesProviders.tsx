import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import React from "react";

import { FeatureName } from "../../../AppRouter";
import { AuthElement } from "../../AuthElement";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import HipaaNotice from "../../HipaaNotice";
import Crumbs, { CrumbsProps } from "../../Crumbs";
import { withCatchAndSuspense } from "../../RSErrorBoundary";

import styles from "./FacilitiesProviders.module.scss";
import FacilitiesProvidersTable from "./FacilitiesProvidersTable";

export function FacilitiesProviders() {
    const crumbProps: CrumbsProps = {
        crumbList: [
            { label: FeatureName.DATA_DASHBOARD, path: "/data-dashboard" },
            { label: FeatureName.FACILITIES_PROVIDERS },
        ],
    };
    return (
        <div className={styles.FacilitiesProviders}>
            <header className="usa-header usa-header--extended padding-top-6 padding-left-2 margin-top-neg-5">
                <Crumbs {...crumbProps}></Crumbs>
                <GridContainer className="margin-left-5 margin-right-7 padding-bottom-8">
                    <div className="font-sans-2xl text-bold">
                        All facilities & providers
                    </div>
                    <hr className="margin-bottom-1" />
                    <div className="font-sans-lg">
                        An index of all the ordering & provider facilities who
                        have submitted to you.
                    </div>
                    <hr />
                </GridContainer>
            </header>
            <GridContainer className="margin-left-7 margin-right-7 padding-top-8">
                <Helmet>
                    <title>{FeatureName.FACILITIES_PROVIDERS}</title>
                </Helmet>
                <article>
                    {withCatchAndSuspense(<FacilitiesProvidersTable />)}
                    <HipaaNotice />
                </article>
            </GridContainer>
        </div>
    );
}

export function FacilitiesProvidersWithAuth() {
    return (
        <AuthElement
            element={withCatchAndSuspense(<FacilitiesProviders />)}
            requiredUserType={MemberType.RECEIVER}
        />
    );
}
