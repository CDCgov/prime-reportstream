import { GridContainer } from "@trussworks/react-uswds";
import React, { useCallback } from "react";

import Crumbs, { CrumbsProps } from "../../components/Crumbs";
import { FeatureName } from "../../utils/FeatureName";
import HipaaNotice from "../../components/HipaaNotice";
import { SenderType } from "../../utils/DataDashboardUtils";
import { EventName, useAppInsightsContext } from "../../contexts/AppInsights";
import { FacilityProviderSubmitterSummary } from "../../components/DataDashboard/FacilityProviderSubmitterDetails/FacilityProviderSubmitterSummary";
import FacilityProviderSubmitterTable from "../../components/DataDashboard/FacilityProviderSubmitterDetails/FacilityProviderSubmitterTable";

import styles from "./FacilityProviderSubmitterDetails.module.scss";

export type FacilityProviderSubmitterDetailsProps = React.PropsWithChildren<{
    senderType: SenderType;
}>;

function FacilityProviderSubmitterDetailsPage(
    props: FacilityProviderSubmitterDetailsProps,
) {
    const featureEvent = `${FeatureName.REPORT_DETAILS} | ${EventName.TABLE_FILTER}`;
    const { appInsights } = useAppInsightsContext();
    const filterClickHandler = useCallback(
        (from: string, to: string) => {
            appInsights?.trackEvent({
                name: featureEvent,
                properties: {
                    tableFilter: { startRange: from, endRange: to },
                },
            });
        },
        [appInsights, featureEvent],
    );
    // TODO: get from params once API is complete.
    // const { senderId } = useParams();
    const senderTypeId = "1234";
    const summaryDetails = {
        senderTypeName: "AFC Urgent Care",
        contactName: "Sid's Pharmacy",
        location: "San Diego, CA",
        phone: "509-332-4608",
        reportDate: "2022-09-28T22:21:33.801667",
        averageTestPerReport: 84,
        totalTests: 84019,
        submitter: "SimpleReport",
        clia: "Not applicable",
    };

    // TODO: Crumbs and title will be dynamic based on prop
    const crumbProps: CrumbsProps = {
        crumbList: [
            { label: FeatureName.DATA_DASHBOARD, path: "/data-dashboard" },
            {
                label: FeatureName.FACILITIES_PROVIDERS,
                path: "/data-dashboard/facilities-providers",
            },
            { label: summaryDetails.senderTypeName },
        ],
    };

    return (
        <div className={styles.FacilityProviderSubmitterDetails}>
            <GridContainer>
                <Crumbs {...crumbProps}></Crumbs>
                <article>
                    <FacilityProviderSubmitterSummary
                        details={summaryDetails || {}}
                        summaryDetailType={props.senderType}
                    />
                    <FacilityProviderSubmitterTable
                        senderTypeId={senderTypeId}
                        senderTypeName={summaryDetails.senderTypeName}
                        onFilterClick={filterClickHandler}
                    />
                    <HipaaNotice />
                </article>
            </GridContainer>
        </div>
    );
}

export default FacilityProviderSubmitterDetailsPage;
