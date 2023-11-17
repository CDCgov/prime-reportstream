import { Helmet } from "react-helmet-async";
import { useCallback } from "react";

import { HeroWrapper } from "../../shared";
import Crumbs, { CrumbsProps } from "../../components/Crumbs";
import HipaaNotice from "../../components/HipaaNotice";
import { FeatureName } from "../../utils/FeatureName";
import { EventName, useAppInsightsContext } from "../../contexts/AppInsights";
import { useOrganizationReceiversFeed } from "../../hooks/UseOrganizationReceiversFeed";
import useReceiverSubmitters from "../../hooks/network/DataDashboard/UseReceiverSubmitters";
import { CustomerStatusType } from "../../utils/DataDashboardUtils";
import Spinner from "../../components/Spinner";
import AdminFetchAlert from "../../components/alerts/AdminFetchAlert";
import { NoServicesBanner } from "../../components/alerts/NoServicesAlert";
import FacilitiesProvidersTable from "../../components/DataDashboard/FacilitiesProviders/FacilitiesProvidersTable";

import styles from "./FacilitiesProviders.module.scss";

export function FacilitiesProvidersPage() {
    const featureEvent = `${FeatureName.FACILITIES_PROVIDERS} | ${EventName.TABLE_FILTER}`;
    const { appInsights } = useAppInsightsContext();
    const {
        isLoading: isFeedLoading,
        data: services,
        activeService,
        setActiveService,
        isDisabled,
    } = useOrganizationReceiversFeed();
    const {
        data: { meta, data: submitters } = {},
        filterManager,
        isLoading: isSubmittersLoading,
    } = useReceiverSubmitters(activeService?.name);
    const filterClickHandler = useCallback(
        (from: string, to: string) => {
            appInsights?.trackEvent({
                name: featureEvent,
                properties: {
                    tableFilter: {
                        startRange: from,
                        endRange: to,
                    },
                },
            });
        },
        [appInsights, featureEvent],
    );
    const isLoading = isFeedLoading || isSubmittersLoading;

    if (isDisabled) return <AdminFetchAlert />;

    if (
        !isLoading &&
        (!activeService ||
            activeService?.customerStatus === CustomerStatusType.INACTIVE)
    )
        return (
            <div className="usa-section margin-bottom-10">
                <NoServicesBanner />
            </div>
        );
    if (isLoading || !submitters || !meta || !services) return <Spinner />;

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
                            {activeService && (
                                <FacilitiesProvidersTable
                                    receiverServices={services}
                                    activeService={activeService}
                                    setActiveService={setActiveService}
                                    filterManager={filterManager}
                                    onFilterClick={filterClickHandler}
                                    submitters={submitters}
                                    pagesTotal={meta.totalPages}
                                    submittersTotal={meta.totalFilteredCount}
                                />
                            )}
                            <HipaaNotice />
                        </article>
                    </section>
                </div>
            </HeroWrapper>
        </div>
    );
}

export default FacilitiesProvidersPage;
