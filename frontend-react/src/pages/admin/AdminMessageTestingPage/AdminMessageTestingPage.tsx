import { GridContainer } from "@trussworks/react-uswds";
import { FormEventHandler, useCallback, useState } from "react";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router";
import { AdminFormWrapper } from "../../../components/Admin/AdminFormWrapper";
import { EditReceiverSettingsParams } from "../../../components/Admin/EditReceiverSettings";
import MessageTestingForm, { RSSubmittedMessage } from "../../../components/Admin/MessageTesting/MessageTestingForm";
import MessageTestingResult from "../../../components/Admin/MessageTesting/MessageTestingResult";
import { warningMessageResult } from "../../../components/Admin/MessageTesting/MessageTestingResult.fixtures";
import Crumbs, { CrumbsProps } from "../../../components/Crumbs";
import Title from "../../../components/Title";
import { RSMessageResult } from "../../../config/endpoints/reports";
import useTestMessages from "../../../hooks/api/messages/UseTestMessages/UseTestMessages";
import { FeatureName } from "../../../utils/FeatureName";

export interface MessageTestingFormValuesInternal {
    testMessage?: string;
    testMessageBody?: string;
}

export interface MessageTestingFormValues {
    testMessage: string;
    testMessageBody: string;
}

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
    // Sets data required for the MessageTestingForm
    const { data, isDisabled } = useTestMessages();
    const [currentTestMessages, setCurrentTestMessages] = useState(data);
    const [customMessageNumber, setCustomMessageNumber] = useState(1);
    const fakeResultData = warningMessageResult;
    const handleSubmit = useCallback<FormEventHandler<HTMLFormElement>>((e) => {
        const formData = Object.fromEntries(
            new FormData(e.currentTarget).entries(),
        ) as unknown as MessageTestingFormValues;

        // TODO: Remove fake result data usage, and Submit formData.testMessageBody to server
        setSubmittedMessage({
            fileName: formData.testMessage,
            reportBody: formData.testMessageBody,
            dateCreated: new Date(),
        });
        setResultData(fakeResultData);
        setCurrentMessageTestStep(MessageTestingSteps.StepTwo);
    }, []);
    // Sets data required for the MessageTestingResult
    const [resultData, setResultData] = useState<RSMessageResult | null>(null);
    const [submittedMessage, setSubmittedMessage] = useState<RSSubmittedMessage | null>(null);

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
                            handleSubmit={handleSubmit}
                        />
                    )}
                    {currentMessageTestStep === MessageTestingSteps.StepTwo && (
                        <MessageTestingResult resultData={resultData} submittedMessage={submittedMessage} />
                    )}
                </GridContainer>
            </AdminFormWrapper>
        </>
    );
};

export default AdminMessageTestingPage;
