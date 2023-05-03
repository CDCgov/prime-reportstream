import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import React from "react";

import { FeatureName } from "../../AppRouter";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { USNavLink } from "../../components/USLink";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

import DashboardTable from "./DashboardTable/DashboardTable";

function Dashboard() {
    return (
        <div className="rs-data-dashboard">
            <header className="usa-header usa-header--extended bg-primary-darker text-white">
                <GridContainer className="margin-left-7 margin-right-7 padding-top-8 padding-bottom-8 rs-max-width-100-important">
                    <div className="font-sans-lg text-blue-30 width-full">
                        Colorado Department of Public Health and Environment
                    </div>
                    <div className="font-sans-2xl">Data Dashboard</div>
                    <hr className="border-1px text-blue-30 margin-bottom-3" />
                    <div className="font-sans-lg">
                        All the labs, facilities, aggregators, etc. that have
                        ever contributed data to your reports.
                    </div>
                    <hr className="border-1px text-blue-30 margin-bottom-3" />
                    <div className="font-sans-2xs">
                        Jump to:{" "}
                        <USNavLink href="/data-dashboard/facilities-providers">
                            All facilities & providers
                        </USNavLink>
                    </div>
                    <hr className="border-1px text-blue-30" />
                </GridContainer>
            </header>
            <GridContainer className="margin-left-7 margin-right-7 padding-top-8 padding-bottom-8 rs-max-width-100-important">
                <Helmet>
                    <title>{FeatureName.DASHBOARD}</title>
                </Helmet>
                <article>{withCatchAndSuspense(<DashboardTable />)}</article>
            </GridContainer>
        </div>
    );
}

export function DashboardWithAuth() {
    return (
        <AuthElement
            element={<Dashboard />}
            requiredUserType={MemberType.RECEIVER}
        />
    );
}
