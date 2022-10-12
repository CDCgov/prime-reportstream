import { useParams } from "react-router-dom";

import HipaaNotice from "../../components/HipaaNotice";
import { useReportsDetail } from "../../hooks/network/History/DeliveryHooks";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { AuthElement } from "../../components/AuthElement";
import { withCatchAndSuspense } from "../../components/RSErrorBoundary";

import Summary from "./Summary";
import ReportDetails from "./ReportDetails";
import FacilitiesTable from "./FacilitiesTable";

const DetailsContent = () => {
    const { reportId } = useParams();
    const { reportDetail } = useReportsDetail(reportId!!);

    return (
        <>
            <Summary report={reportDetail} />
            <ReportDetails report={reportDetail} />
            {withCatchAndSuspense(<FacilitiesTable reportId={reportId!!} />)}
            <HipaaNotice />
        </>
    );
};

export const Details = () => withCatchAndSuspense(<DetailsContent />);
export const DetailsWithAuth = () => (
    <AuthElement element={<Details />} requiredUserType={MemberType.RECEIVER} />
);
