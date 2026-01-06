import { GridContainer } from "@trussworks/react-uswds";
import { PropsWithChildren } from "react";

import styles from "./FacilityProviderSubmitterDetails.module.scss";
import { FacilityProviderSubmitterSummary } from "./FacilityProviderSubmitterSummary";
import FacilityProviderSubmitterTable from "./FacilityProviderSubmitterTable";
import { SenderType } from "../../../utils/DataDashboardUtils";
import { FeatureName } from "../../../utils/FeatureName";
import Crumbs, { CrumbsProps } from "../../Crumbs";
import HipaaNotice from "../../HipaaNotice";

export type FacilityProviderSubmitterDetailsProps = PropsWithChildren<{
    senderType: SenderType;
}>;

function FacilityProviderSubmitterDetailsPage(
    props: FacilityProviderSubmitterDetailsProps,
) {
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
                    />
                    <HipaaNotice />
                </article>
            </GridContainer>
        </div>
    );
}

export default FacilityProviderSubmitterDetailsPage;
