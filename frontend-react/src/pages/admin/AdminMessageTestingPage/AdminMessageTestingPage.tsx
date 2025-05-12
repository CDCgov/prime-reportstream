import { Button, GridContainer } from "@trussworks/react-uswds";
import { useRef, useState } from "react";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router";
import { AdminFormWrapper } from "../../../components/Admin/AdminFormWrapper";
import { EditReceiverSettingsParams } from "../../../components/Admin/EditReceiverSettings";
import MessageTestingForm from "../../../components/Admin/MessageTesting/MessageTestingForm";
import MessageTestingResult from "../../../components/Admin/MessageTesting/MessageTestingResult";
import Crumbs, { CrumbsProps } from "../../../components/Crumbs";
import Spinner from "../../../components/Spinner";
import Title from "../../../components/Title";
import { RSMessage } from "../../../config/endpoints/reports";
import useTestMessageResult from "../../../hooks/api/messages/UseTestMessageResult/UseTestMessageResult";
import useTestMessages from "../../../hooks/api/messages/UseTestMessages/UseTestMessages";
import useTestMessageSenders from "../../../hooks/api/messages/UseTestMessageSenders/UseTestMessageSenders";
import { Alert } from "../../../shared";
import { FeatureName } from "../../../utils/FeatureName";

export interface MessageTestingFormValuesInternal {
    testMessage?: string;
    testMessageBody?: string;
}

export interface MessageTestingFormValues {
    testMessage: string;
    testMessageBody: string;
}

enum MessageTestingSteps {
    StepOne = "MessageTestSelection",
    StepTwo = "MessageTestResults",
    StepFail = "MessageTestFail",
}

const AdminMessageTestingPage = () => {
    const lastBodyRef = useRef<RSMessage | null>(null);
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

    // Sets step of the Message Testing process laid out in the MessageTestingSteps enum
    const [currentMessageTestStep, setCurrentMessageTestStep] = useState<MessageTestingSteps>(
        MessageTestingSteps.StepOne,
    );

    // Sets data required for the MessageTestingForm
    const { data: messageData } = useTestMessages();
    // Sets the possible Senders to use in the custom message dropdown
    const { data: senderData } = useTestMessageSenders();
    const { mutate, isPending, data: testResultData, error: apiError } = useTestMessageResult();
    const [selectedOption, setSelectedOption] = useState<RSMessage | null>(null);
    const [currentTestMessages, setCurrentTestMessages] = useState<RSMessage[]>(messageData);
    const handleSubmit = () => {
        const testMessage = selectedOption ?? null;
        lastBodyRef.current = testMessage;
        mutate(
            { testMessage: testMessage },
            {
                onSuccess: () => setCurrentMessageTestStep(MessageTestingSteps.StepTwo),
                onError: () => setCurrentMessageTestStep(MessageTestingSteps.StepFail),
            },
        );
    };

    // Custom refetch method that uses the useRef reference of the previous
    // state of the selectedOption and passes it to the useTestMessageResult hook
    const refetch = () => (lastBodyRef.current !== null ? mutate({ testMessage: lastBodyRef.current }) : undefined);

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
                    {isPending ? (
                        <Spinner />
                    ) : (
                        <>
                            {currentMessageTestStep === MessageTestingSteps.StepFail && (
                                <>
                                    <Alert type="error" className="margin-bottom-10" heading="Test message failed">
                                        {apiError?.message ?? "An unknown error occurred while testing the message."}
                                    </Alert>
                                    <Button
                                        type="button"
                                        onClick={() => setCurrentMessageTestStep(MessageTestingSteps.StepOne)}
                                    >
                                        Try another message
                                    </Button>
                                </>
                            )}
                            {currentMessageTestStep === MessageTestingSteps.StepOne && (
                                <MessageTestingForm
                                    currentTestMessages={currentTestMessages}
                                    setSelectedOption={setSelectedOption}
                                    setCurrentTestMessages={setCurrentTestMessages}
                                    handleSubmit={handleSubmit}
                                    selectedOption={selectedOption}
                                    senderData={senderData}
                                />
                            )}
                            {currentMessageTestStep === MessageTestingSteps.StepTwo && testResultData && (
                                <MessageTestingResult
                                    resultData={testResultData}
                                    submittedMessage={selectedOption}
                                    handleGoBack={() => {
                                        setSelectedOption(null);
                                        setCurrentMessageTestStep(MessageTestingSteps.StepOne);
                                    }}
                                    refetch={refetch}
                                    orgname={orgname ?? "N/A"}
                                    receivername={receivername ?? "N/A"}
                                />
                            )}
                        </>
                    )}
                </GridContainer>
            </AdminFormWrapper>
        </>
    );
};

export default AdminMessageTestingPage;
