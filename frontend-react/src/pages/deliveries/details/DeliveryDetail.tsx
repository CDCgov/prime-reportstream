import { GridContainer } from "@trussworks/react-uswds";
import { useParams } from "react-router-dom";

import DeliveryFacilitiesTable from "./DeliveryFacilitiesTable";
import DeliveryInfo from "./DeliveryInfo";
import Summary from "./Summary";
import HipaaNotice from "../../../components/HipaaNotice";
import { withCatchAndSuspense } from "../../../components/RSErrorBoundary/RSErrorBoundary";
import useReportsDetail from "../../../hooks/api/deliveries/UseReportDetail/UseReportDetail";

const DetailsContent = () => {
    const { reportId } = useParams();
    const { data: reportDetail } = useReportsDetail(reportId!);

    return (
        <GridContainer>
            <article>
                <Summary report={reportDetail} />
                <DeliveryInfo report={reportDetail} />
                {withCatchAndSuspense(
                    <DeliveryFacilitiesTable reportId={reportId!} />,
                )}
                <HipaaNotice />
            </article>
        </GridContainer>
    );
};

export const DeliveryDetailPage = () =>
    withCatchAndSuspense(<DetailsContent />);

export default DeliveryDetailPage;
