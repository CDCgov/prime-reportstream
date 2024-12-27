import { GridContainer } from "@trussworks/react-uswds";
import { useState } from "react";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router";
import { AdminFormWrapper } from "../../../components/Admin/AdminFormWrapper";
import { EditReceiverSettingsParams } from "../../../components/Admin/EditReceiverSettings";
import MessageTestingForm from "../../../components/Admin/MessageTesting/MessageTestingForm";
import Crumbs, { CrumbsProps } from "../../../components/Crumbs";
import Title from "../../../components/Title";
import useTestMessages from "../../../hooks/api/messages/UseTestMessages/UseTestMessages";
import { FeatureName } from "../../../utils/FeatureName";

const AdminMessageTestingPage = () => {
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
    enum MessageTestingSteps {
        StepOne = "MessageTestSelection",
        StepTwo = "MessageTestResults",
    }

    // Sets which step of the Message Testing process the user is at
    const [currentMessageTestStep, setCurrentMessageTestStep] = useState(MessageTestingSteps.StepOne);
    const { data, isDisabled } = useTestMessages();
    const [currentTestMessages, setCurrentTestMessages] = useState(data);
    const [customMessageNumber, setCustomMessageNumber] = useState(1);

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
                <GridContainer>
                    <p>
                        Test a message from the message bank or by entering a custom message. You can view test results
                        in this window while you are logged in. To save for later reference, you can open messages, test
                        results and output messages in separate tabs.
                    </p>
                    <hr />
                    <p className="font-sans-xl text-bold">Test message bank</p>
                    {currentMessageTestStep === MessageTestingSteps.StepOne && (
                        <MessageTestingForm
                            isDisabled={isDisabled}
                            currentTestMessages={currentTestMessages}
                            setCurrentTestMessages={setCurrentTestMessages}
                            customMessageNumber={customMessageNumber}
                            setCustomMessageNumber={setCustomMessageNumber}
                        />
                    )}
                    {currentMessageTestStep === MessageTestingSteps.StepTwo && <MessageTestingResult />}
                </GridContainer>
            </AdminFormWrapper>
        </>
    );
};

export default AdminMessageTestingPage;
