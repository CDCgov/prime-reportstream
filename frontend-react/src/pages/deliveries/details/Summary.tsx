import Crumbs, { CrumbsProps } from "../../../components/Crumbs";
import Title from "../../../components/Title";
import { RSDelivery } from "../../../config/endpoints/deliveries";
import useOrganizationSettings from "../../../hooks/api/organizations/UseOrganizationSettings/UseOrganizationSettings";
import { FeatureName } from "../../../utils/FeatureName";
import ReportLink from "../daily-data/ReportLink";

interface Props {
    /* REQUIRED
    Passing in a report allows this component to extract key properties (id)
    and display them on the DeliveryDetail page. */
    report: RSDelivery | undefined;
}

function Summary(props: Props) {
    const { report }: Props = props;
    const { data: orgDetails } = useOrganizationSettings();
    const { description } = orgDetails ?? {};
    const crumbProps: CrumbsProps = {
        crumbList: [
            { label: FeatureName.DAILY_DATA, path: "/daily-data" },
            { label: "Details" },
        ],
    };

    return (
        <div className="grid-row tablet:margin-top-6">
            <div className="grid-col-fill">
                <Crumbs {...crumbProps} />
                <Title preTitle={description} title={report?.reportId ?? ""} />
            </div>
            <div className="grid-col-auto margin-bottom-5 margin-top-auto">
                <ReportLink
                    reportId={report!.reportId}
                    reportExpires={report!.expires}
                    fileType={report?.fileType}
                    button
                />
            </div>
        </div>
    );
}

export default Summary;
