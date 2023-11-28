import { useParams } from "react-router-dom";
import { GridContainer } from "@trussworks/react-uswds";

import HipaaNotice from "../../../components/HipaaNotice";
import { useReportsDetail } from "../../../hooks/network/History/DeliveryHooks";
import { RSDelivery } from "../../../config/endpoints/deliveries";

import Summary from "./Summary";
import DeliveryInfo from "./DeliveryInfo";
import DeliveryFacilitiesTable from "./DeliveryFacilitiesTable";

export interface DeliveryDetailBaseProps extends React.PropsWithChildren {
    report: RSDelivery;
}

export function DeliveryDetailBase({
    report,
    children,
}: DeliveryDetailBaseProps) {
    return (
        <article>
            <Summary report={report} />
            <DeliveryInfo report={report} />
            <DeliveryFacilitiesTable reportId={report.reportId} />
            <HipaaNotice />
            {children}
        </article>
    );
}

function DeliveryDetailPage() {
    const { reportId } = useParams();
    const { data: report } = useReportsDetail(reportId!);
    return (
        <GridContainer>
            <DeliveryDetailBase report={report!} />
        </GridContainer>
    );
}

export default DeliveryDetailPage;
