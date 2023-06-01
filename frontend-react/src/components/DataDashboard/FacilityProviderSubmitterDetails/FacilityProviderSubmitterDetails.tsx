import { GridContainer } from "@trussworks/react-uswds";
import React from "react";

import { AuthElement } from "../../AuthElement";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import Crumbs, { CrumbsProps } from "../../Crumbs";
import { FeatureName } from "../../../AppRouter";
import HipaaNotice from "../../HipaaNotice";
import { AggregatorType } from "../../../utils/DataDashboardUtils";
import { withCatchAndSuspense } from "../../RSErrorBoundary";

import styles from "./FacilityProviderSubmitterDetails.module.scss";
import { FacilityProviderSubmitterSummary } from "./FacilityProviderSubmitterSummary";
import FacilityProviderSubmitterTable from "./FacilityProviderSubmitterTable";

export type FacilityProviderSubmitterDetailsProps = React.PropsWithChildren<{
    aggregatorType: AggregatorType;
}>;

function FacilityProviderSubmitterDetails(
    props: FacilityProviderSubmitterDetailsProps
) {
    // TODO: get from params once API is complete.
    // const { aggregatorId } = useParams();
    const aggregatorTypeId = "1234";
    const summaryDetails = {
        aggregatorTypeName: "AFC Urgent Care",
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
            { label: summaryDetails.aggregatorTypeName },
        ],
    };

    return (
        <div className={styles.FacilityProviderSubmitterDetails}>
            <GridContainer>
                <Crumbs {...crumbProps}></Crumbs>
                <article>
                    <FacilityProviderSubmitterSummary
                        details={summaryDetails || {}}
                        summaryDetailType={props.aggregatorType}
                    />
                    <FacilityProviderSubmitterTable
                        aggregatorTypeId={aggregatorTypeId!!}
                        aggregatorTypeName={summaryDetails.aggregatorTypeName}
                    />
                    <HipaaNotice />
                </article>
            </GridContainer>
        </div>
    );
}

export function FacilityProviderSubmitterDetailsWithAuth(
    props: FacilityProviderSubmitterDetailsProps
) {
    return (
        <AuthElement
            element={withCatchAndSuspense(
                <FacilityProviderSubmitterDetails {...props} />
            )}
            requiredUserType={MemberType.RECEIVER}
        />
    );
}
