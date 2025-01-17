import { GridContainer } from "@trussworks/react-uswds";
import { useRef, useState } from "react";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router";
import html2canvas from "html2canvas";
import jsPDF from "jspdf";
import { AdminFormWrapper } from "../../../components/Admin/AdminFormWrapper";
import { EditReceiverSettingsParams } from "../../../components/Admin/EditReceiverSettings";
import MessageTestingForm from "../../../components/Admin/MessageTesting/MessageTestingForm";
import MessageTestingResult from "../../../components/Admin/MessageTesting/MessageTestingResult";
import Crumbs, { CrumbsProps } from "../../../components/Crumbs";
import Spinner from "../../../components/Spinner";
import Title from "../../../components/Title";
import { RSMessage, RSMessageResult } from "../../../config/endpoints/reports";
import useTestMessageResult from "../../../hooks/api/messages/UseTestMessageResult/UseTestMessageResult";
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

enum MessageTestingSteps {
    StepOne = "MessageTestSelection",
    StepTwo = "MessageTestResults",
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

    // Sets step of the Message Testing process laid out in the MessageTestingSteps enum
    const [currentMessageTestStep, setCurrentMessageTestStep] = useState<MessageTestingSteps>(
        MessageTestingSteps.StepOne,
    );

    // Sets data required for the MessageTestingForm
    const { data: messageData } = useTestMessages();
    const { setRequestBody, isLoading, data: testResultData, refetch } = useTestMessageResult();
    const [selectedOption, setSelectedOption] = useState<RSMessage | null>(null);
    const [currentTestMessages, setCurrentTestMessages] = useState<RSMessage[]>(messageData);
    const [isGeneratingPDF, setIsGeneratingPDF] = useState(false);
    const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setRequestBody(selectedOption?.reportBody ? selectedOption.reportBody : null);
        setCurrentMessageTestStep(MessageTestingSteps.StepTwo);
    };
    const pdfRef = useRef<HTMLDivElement>(null);
    const handleSaveToPDF = async () => {
        if (!pdfRef.current) return;

        try {
            setIsGeneratingPDF(true);
            // Use html2canvas on the referenced DOM node
            const canvas = await html2canvas(pdfRef.current, {
                // Optional: increase scale for better quality
                scale: 2,
            });

            const imgData = canvas.toDataURL("image/png");

            const pdf = new jsPDF("p", "pt", "letter");
            // A4-ish or Letter size in points (72 pt = 1 inch)
            const pageWidth = 612; // ~8.5" x 72
            const pageHeight = 792; // ~11" x 72

            // We want some margin around the image
            const marginX = 20;
            const marginY = 20;

            let imgWidth = pageWidth - marginX * 2;
            let imgHeight = (canvas.height * imgWidth) / canvas.width;

            // If the resulting height is still larger than the page height, scale again
            if (imgHeight > pageHeight - marginY * 2) {
                imgHeight = pageHeight - marginY * 2;
                imgWidth = (canvas.width * imgHeight) / canvas.height;
            }
            pdf.addImage(imgData, "PNG", marginX, marginY, imgWidth, imgHeight);
            pdf.save(`${orgname}-${receivername}-results.pdf`);
        } finally {
            setIsGeneratingPDF(false);
        }
    };

    return (
        <div ref={pdfRef}>
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
                    {isLoading || isGeneratingPDF ? (
                        <Spinner />
                    ) : (
                        <>
                            {currentMessageTestStep === MessageTestingSteps.StepOne && (
                                <MessageTestingForm
                                    currentTestMessages={currentTestMessages}
                                    setSelectedOption={setSelectedOption}
                                    setCurrentTestMessages={setCurrentTestMessages}
                                    handleSubmit={handleSubmit}
                                    selectedOption={selectedOption}
                                />
                            )}
                            {currentMessageTestStep === MessageTestingSteps.StepTwo && (
                                <MessageTestingResult
                                    resultData={testResultData as RSMessageResult}
                                    submittedMessage={selectedOption}
                                    handleGoBack={() => {
                                        setRequestBody(null);
                                        setCurrentMessageTestStep(MessageTestingSteps.StepOne);
                                    }}
                                    refetch={refetch}
                                    handleSaveToPDF={handleSaveToPDF}
                                />
                            )}
                        </>
                    )}
                </GridContainer>
            </AdminFormWrapper>
        </div>
    );
};

export default AdminMessageTestingPage;
