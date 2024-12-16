import { Button, GridContainer } from "@trussworks/react-uswds";
import { ChangeEvent, useState } from "react";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router";
import { CustomMessage } from "./CustomMessage";
import { RadioField } from "./RadioField";
import useReportTesting from "../../../hooks/api/messages/UseMessageTesting";
import { FeatureName } from "../../../utils/FeatureName";
import AdminFetchAlert from "../../alerts/AdminFetchAlert";
import Crumbs, { CrumbsProps } from "../../Crumbs";
import Spinner from "../../Spinner";
import Title from "../../Title";
import { AdminFormWrapper } from "../AdminFormWrapper";
import { EditReceiverSettingsParams } from "../EditReceiverSettings";

function ReportTesting() {
    const { orgname, receivername } = useParams<EditReceiverSettingsParams>();
    const { testMessages, isDisabled, isLoading } = useReportTesting();
    const [selectedOption, setSelectedOption] = useState<string | null>(null);
    const [currentTestMessages, setCurrentTestMessages] = useState(testMessages);
    const [openCustomMessage, setOpenCustomMessage] = useState(false);
    const [customMessageNumber, setCustomMessageNumber] = useState(1);
    const crumbProps: CrumbsProps = {
        crumbList: [
            {
                label: FeatureName.RECEIVER_SETTINGS,
                path: `/admin/orgreceiversettings/org/${orgname}/receiver/${receivername}/action/edit`,
            },
            { label: FeatureName.MESSAGE_TESTING },
        ],
    };

    if (isDisabled) {
        return <AdminFetchAlert />;
    }
    if (isLoading || !currentTestMessages) return <Spinner />;

    const handleSelect = (event: ChangeEvent<HTMLInputElement>) => {
        setSelectedOption(event.target.value);
    };

    const handleAddCustomMessage = () => {
        setSelectedOption(null);
        setOpenCustomMessage(true);
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
                <GridContainer>
                    <p>
                        Test a message from the message bank or by entering a custom message. You can view test results
                        in this window while you are logged in. To save for later reference, you can open messages, test
                        results and output messages in separate tabs.
                    </p>
                    <hr />
                    <p className="font-sans-xl text-bold">Test message bank</p>
                    <form>
                        <fieldset className="usa-fieldset bg-base-lightest padding-3">
                            {currentTestMessages?.map((item, index) => (
                                <RadioField
                                    key={index}
                                    index={index}
                                    title={item.fileName}
                                    body={item.reportBody}
                                    handleSelect={handleSelect}
                                    selectedOption={selectedOption}
                                />
                            ))}
                            {openCustomMessage && (
                                <CustomMessage
                                    customMessageNumber={customMessageNumber}
                                    currentTestMessages={currentTestMessages}
                                    setCustomMessageNumber={setCustomMessageNumber}
                                    setCurrentTestMessages={setCurrentTestMessages}
                                    setOpenCustomMessage={setOpenCustomMessage}
                                />
                            )}
                        </fieldset>
                        <div className="padding-top-4">
                            <Button type="button" outline onClick={handleAddCustomMessage}>
                                Test custom message
                            </Button>
                            <Button disabled={!selectedOption} type="button">
                                Run test
                            </Button>
                        </div>
                    </form>
                </GridContainer>
            </AdminFormWrapper>
        </>
    );
}

export default ReportTesting;
