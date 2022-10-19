import { useParams } from "react-router-dom";

import HipaaNotice from "../../../components/HipaaNotice";
import { useReportsDetail } from "../../../hooks/network/History/DeliveryHooks";
import { MemberType } from "../../../hooks/UseOktaMemberships";
import { AuthElement } from "../../../components/AuthElement";
import { withCatchAndSuspense } from "../../../components/RSErrorBoundary";

import Summary from "./Summary";
import DeliveryInfo from "./DeliveryInfo";
import DeliveryFacilitiesTable from "./DeliveryFacilitiesTable";

const DetailsContent = () => {
    const { reportId } = useParams();
    const { reportDetail } = useReportsDetail(reportId!!);

    return (
        <>
            <Summary report={reportDetail} />
            <DeliveryInfo report={reportDetail} />
            {withCatchAndSuspense(
                <DeliveryFacilitiesTable reportId={reportId!!} />
            )}
            <HipaaNotice />
        </>
    );
};

export const DeliveryDetail = () => withCatchAndSuspense(<DetailsContent />);
export const DeliveryDetailWithAuth = () => (
    <AuthElement
        element={<DeliveryDetail />}
        requiredUserType={MemberType.RECEIVER}
    />
);
