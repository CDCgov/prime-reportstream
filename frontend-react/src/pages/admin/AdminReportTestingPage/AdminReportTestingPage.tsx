import { GridContainer } from "@trussworks/react-uswds";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router";
import { AdminFormWrapper } from "../../../components/Admin/AdminFormWrapper";
import { EditReceiverSettingsParams } from "../../../components/Admin/EditReceiverSettings";
import MessageTestingBody from "../../../components/Admin/MessageTesting/MessageTestingBody";
import Crumbs, { CrumbsProps } from "../../../components/Crumbs";
import Title from "../../../components/Title";
import { FeatureName } from "../../../utils/FeatureName";

const AdminReportTestingPage = () => {
    const { orgname, receivername } = useParams<EditReceiverSettingsParams>();
    const crumbProps: CrumbsProps = {
        crumbList: [
            {
                label: FeatureName.RECEIVER_SETTINGS,
                path: `/admin/orgreceiversettings/org/${orgname}/receiver/${receivername}/action/edit`,
            },
            { label: FeatureName.MESSAGE_TESTING },
        ],
    };
    return (
        <>
            <Helmet>
                <title>Message testing - ReportStream</title>
            </Helmet>
            <GridContainer>
                <Crumbs {...crumbProps}></Crumbs>
            </GridContainer>
            <AdminFormWrapper
                header={
                    <>
                        <Title title={"Message testing"} />
                        <h2 className="margin-bottom-0">
                            <span className="text-normal font-body-md text-base margin-bottom-0">
                                Org name: {orgname}
                                <br />
                                Receiver name: {receivername}
                            </span>
                        </h2>
                    </>
                }
            >
                <MessageTestingBody />
            </AdminFormWrapper>
        </>
    );
};

export default AdminReportTestingPage;
