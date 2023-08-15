import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import React from "react";

import { FeatureName } from "../../AppRouter";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { USNavLink } from "../../components/USLink";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import DataDashboardTable from "../../components/DataDashboard/DataDashboardTable/DataDashboardTable";
import HipaaNotice from "../../components/HipaaNotice";

import styles from "./DataDashboard.module.scss";

function DataDashboard() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails || {};
    return (
        <div className={styles.DataDashboard}>
            <header className="usa-header usa-header--extended bg-primary-darker text-white margin-top-neg-5">
                <GridContainer className="margin-left-7 margin-right-7 padding-top-8 padding-bottom-8 rs-max-width-100-important">
                    <div className="font-sans-lg text-blue-30 width-full">
                        {description}
                    </div>
                    <div className="font-sans-2xl text-bold">
                        Data Dashboard
                    </div>
                    <hr className="margin-bottom-3" />
                    <div className="font-sans-lg">
                        All the labs, facilities, aggregators, etc. that have
                        ever contributed data to your reports.
                    </div>
                    <hr className="margin-bottom-3" />
                    <div className="font-sans-2xs">
                        Jump to:{" "}
                        <USNavLink href="/data-dashboard/facilities-providers">
                            All facilities & providers
                        </USNavLink>
                    </div>
                    <hr />
                </GridContainer>
            </header>
            <GridContainer className="margin-left-7 margin-right-7 padding-top-8 rs-max-width-100-important">
                <Helmet>
                    <title>{FeatureName.DATA_DASHBOARD}</title>
                </Helmet>
                <article>
                    {withCatchAndSuspense(<DataDashboardTable />)}
                    <HipaaNotice />
                </article>
            </GridContainer>
        </div>
    );
}

export function DataDashboardWithAuth() {
    return (
        <AuthElement
            element={<DataDashboard />}
            requiredUserType={MemberType.RECEIVER}
        />
    );
}
