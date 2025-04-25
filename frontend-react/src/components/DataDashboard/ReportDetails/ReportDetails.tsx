import { GridContainer } from "@trussworks/react-uswds";
import { useParams } from "react-router-dom";

import styles from "./ReportDetails.module.scss";
import { ReportDetailsSummary } from "./ReportDetailsSummary";
import ReportDetailsTable from "./ReportDetailsTable";
import useReportsDetail from "../../../hooks/api/deliveries/UseReportDetail/UseReportDetail";
import { FeatureName } from "../../../utils/FeatureName";
import Crumbs, { CrumbsProps } from "../../Crumbs";
import HipaaNotice from "../../HipaaNotice";
import { withCatchAndSuspense } from "../../RSErrorBoundary/RSErrorBoundary";

export function ReportDetailsPage() {
    const crumbProps: CrumbsProps = {
        crumbList: [
            { label: FeatureName.DATA_DASHBOARD, path: "/data-dashboard" },
            { label: FeatureName.REPORT_DETAILS },
        ],
    };
    const { reportId } = useParams();
    const { data: reportDetail } = useReportsDetail(reportId!);

    return (
        <div className={styles.ReportDetails}>
            <header className="usa-header usa-header--extended padding-left-4 padding-top-4 margin-top-neg-5">
                <Crumbs {...crumbProps}></Crumbs>
            </header>
            <GridContainer className="rs-max-width-100-important">
                <article>
                    <ReportDetailsSummary report={reportDetail} />
                    {withCatchAndSuspense(<ReportDetailsTable reportId={reportId!} />)}
                    <HipaaNotice />
                </article>
            </GridContainer>
        </div>
    );
}

export default ReportDetailsPage;
