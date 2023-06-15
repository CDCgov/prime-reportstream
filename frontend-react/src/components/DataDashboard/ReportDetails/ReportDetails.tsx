import { GridContainer } from "@trussworks/react-uswds";
import React from "react";
import { useParams } from "react-router-dom";

import { FeatureName } from "../../../AppRouter";
import { AuthElement } from "../../AuthElement";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import Crumbs, { CrumbsProps } from "../../Crumbs";
import { useReportsDetail } from "../../../hooks/network/DataDashboard/DataDashboardHooks";
import { withCatchAndSuspense } from "../../RSErrorBoundary";
import HipaaNotice from "../../HipaaNotice";

import styles from "./ReportDetails.module.scss";
import { ReportDetailsSummary } from "./ReportDetailsSummary";
import ReportDetailsTable from "./ReportDetailsTable";

function ReportDetails() {
    const crumbProps: CrumbsProps = {
        crumbList: [
            { label: FeatureName.DATA_DASHBOARD, path: "/data-dashboard" },
            { label: FeatureName.REPORT_DETAILS },
        ],
    };
    const { reportId } = useParams();
    const { data: reportDetail } = useReportsDetail(reportId!!);

    return (
        <div className={styles.ReportDetails}>
            <GridContainer className="rs-max-width-100-important">
                <Crumbs {...crumbProps}></Crumbs>
                <article>
                    <ReportDetailsSummary report={reportDetail} />
                    {withCatchAndSuspense(
                        <ReportDetailsTable reportId={reportId!!} />
                    )}
                    <HipaaNotice />
                </article>
            </GridContainer>
        </div>
    );
}

export function ReportDetailsWithAuth() {
    return (
        <AuthElement
            element={withCatchAndSuspense(<ReportDetails />)}
            requiredUserType={MemberType.RECEIVER}
        />
    );
}
