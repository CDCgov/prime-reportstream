import { useParams } from "react-router-dom";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../../components/HipaaNotice";
import { useReportsDetail } from "../../../hooks/network/History/DeliveryHooks";
import { withCatchAndSuspense } from "../../../components/RSErrorBoundary";

import Summary from "./Summary";
import DeliveryInfo from "./DeliveryInfo";
import DeliveryFacilitiesTable from "./DeliveryFacilitiesTable";

const DetailsContent = () => {
    const { reportId } = useParams();
    const { data: reportDetail } = useReportsDetail(reportId!!);

    return (
        <GridContainer>
            <article>
                <Summary report={reportDetail} />
                <DeliveryInfo report={reportDetail} />
                {withCatchAndSuspense(
                    <DeliveryFacilitiesTable reportId={reportId!!} />,
                )}
                <HipaaNotice />
            </article>
        </GridContainer>
    );
};

export const DeliveryDetailPage = () =>
    withCatchAndSuspense(<DetailsContent />);

export default DeliveryDetailPage;
