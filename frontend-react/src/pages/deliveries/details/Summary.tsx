import ReportLink from "../Table/ReportLink";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import Crumbs, { CrumbsProps } from "../../../components/Crumbs";
import Title from "../../../components/Title";
import { useOrganizationSettings } from "../../../hooks/UseOrganizationSettings";

interface Props {
    /* REQUIRED
    Passing in a report allows this component to extract key properties (id) 
    and display them on the DeliveryDetail page. */
    report: RSDelivery | undefined;
}

function Summary(props: Props) {
    const { report }: Props = props;
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails || {};
    const crumbProps: CrumbsProps = {
        crumbList: [
            { label: "Daily Data", path: "/daily-data" },
            { label: "Details" },
        ],
        noPadding: true,
    };

    return (
        <div className="grid-container grid-row tablet:margin-top-6">
            <div className="grid-col-fill">
                <Crumbs {...crumbProps} />
                <Title preTitle={description} title={report?.reportId || ""} />
            </div>
            <div className="grid-col-auto margin-bottom-5 margin-top-auto">
                <ReportLink report={report} button />
            </div>
        </div>
    );
}

export default Summary;
