import { Helmet } from "react-helmet-async";
import React from "react";

import { FeatureName } from "../../../utils/FeatureName";
import { AuthElement } from "../../AuthElement";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import HipaaNotice from "../../HipaaNotice";
import Crumbs, { CrumbsProps } from "../../Crumbs";
import { withCatchAndSuspense } from "../../RSErrorBoundary";
import { HeroWrapper } from "../../../shared";

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
            <div className="bg-primary-lighter">
                <div className="grid-container">
                    <header className="usa-section usa-prose">
                        <Crumbs {...crumbProps}></Crumbs>
                        <div className="font-sans-2xl text-bold">
                            All facilities & providers
                        </div>
                        <hr className="margin-bottom-1" />
                        <div className="font-sans-lg">
                            An index of all the ordering & provider facilities
                            who have submitted to you.
                        </div>
                        <hr />
                    </header>
                </div>
            </div>
            <HeroWrapper>
                <div className="grid-container">
                    <section className="usa-section">
                        <Helmet>
                            <title>{FeatureName.FACILITIES_PROVIDERS}</title>
                        </Helmet>
                        <article>
                            {withCatchAndSuspense(<FacilitiesProvidersTable />)}
                            <HipaaNotice />
                        </article>
                    </section>
                </div>
            </HeroWrapper>
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
