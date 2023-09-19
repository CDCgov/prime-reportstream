import { Helmet } from "react-helmet-async";
import React from "react";

import { FeatureName } from "../../utils/FeatureName";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { USNavLink } from "../../components/USLink";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";
import { useOrganizationSettings } from "../../hooks/UseOrganizationSettings";
import DataDashboardTable from "../../components/DataDashboard/DataDashboardTable/DataDashboardTable";
import HipaaNotice from "../../components/HipaaNotice";
import { HeroWrapper } from "../../shared";

import styles from "./DataDashboard.module.scss";

function DataDashboard() {
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails || {};
    return (
        <div className={styles.DataDashboard}>
            <div className="bg-primary-darker text-white">
                <div className="grid-container">
                    <header className="usa-section usa-prose">
                        <div className="font-sans-lg text-blue-30">
                            {description}
                        </div>
                        <div className="font-sans-2xl text-bold">
                            Data Dashboard
                        </div>
                        <hr className="margin-bottom-3" />
                        <div className="font-sans-lg">
                            All the labs, facilities, aggregators, etc. that
                            have ever contributed data to your reports.
                        </div>
                        <hr className="margin-bottom-3" />
                        <div className="font-sans-2xs">
                            Jump to:{" "}
                            <USNavLink href="/data-dashboard/facilities-providers">
                                All facilities & providers
                            </USNavLink>
                        </div>
                        <hr />
                    </header>
                </div>
            </div>
            <HeroWrapper>
                <div className="grid-container">
                    <section className="usa-section">
                        <Helmet>
                            <title>{FeatureName.DATA_DASHBOARD}</title>
                        </Helmet>
                        <article>
                            {withCatchAndSuspense(<DataDashboardTable />)}
                            <HipaaNotice />
                        </article>
                    </section>
                </div>
            </HeroWrapper>
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
