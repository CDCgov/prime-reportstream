import { Button, GridContainer, Textarea } from "@trussworks/react-uswds";
import { ChangeEvent, useState } from "react";
import { Helmet } from "react-helmet-async";
import { useParams } from "react-router";
import { AdminFormWrapper } from "./AdminFormWrapper";
import { EditReceiverSettingsParams } from "./EditReceiverSettings";
import useReportTesting from "../../hooks/api/messages/UseMessageTesting";
import { Icon } from "../../shared";
import AdminFetchAlert from "../alerts/AdminFetchAlert";
import Spinner from "../Spinner";
import Title from "../Title";

function ReportTesting() {
    const { orgname, receivername } = useParams<EditReceiverSettingsParams>();
    const { testMessages, isDisabled, isLoading } = useReportTesting();
    const [selectedOption, setSelectedOption] = useState<string | null>(null);
    const [currentTestMessages, setCurrentTestMessages] = useState(testMessages);
    const [openCustomMessage, setOpenCustomMessage] = useState(false);
    const [customMessageNumber, setCustomMessageNumber] = useState(1);

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

    const CustomMessage = () => {
        const [text, setText] = useState("");
        const handleTextareaChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
            setText(event.target.value);
        };
        const handleAddCustomMessage = () => {
            const dateCreated = new Date();
            setCurrentTestMessages([
                ...currentTestMessages,
                {
                    dateCreated: dateCreated.toString(),
                    fileName: `Custom message ${customMessageNumber}`,
                    reportBody: text,
                },
            ]);
            setCustomMessageNumber(customMessageNumber + 1);
            setText("");
            setOpenCustomMessage(false);
        };

        return (
            <div className="width-full">
                <p className="text-bold">Enter custom message</p>
                <p>Custom messages do not save to the bank after you log out.</p>
                <Textarea
                    value={text}
                    onChange={handleTextareaChange}
                    id="custom-message-text"
                    name="custom-message-text"
                    className="width-full maxw-full margin-bottom-205"
                />
                <div className="width-full text-right">
                    <Button
                        type="button"
                        outline
                        onClick={() => {
                            setOpenCustomMessage(false);
                        }}
                    >
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleAddCustomMessage} disabled={text.length === 0}>
                        Add
                    </Button>
                </div>
            </div>
        );
    };

    const RadioField = ({ title, body, index }: { title: string; body: string; index: number }) => {
        const openTextInNewTab = () => {
            let formattedContent = body;

            // Check if the content is JSON and format it
            try {
                formattedContent = JSON.stringify(JSON.parse(body), null, 2);
            } catch {
                formattedContent = body;
            }

            const blob = new Blob([formattedContent], { type: "text/plain" });

            const url = URL.createObjectURL(blob);

            window.open(url, "_blank");

            // Revoke the URL to free up memory
            URL.revokeObjectURL(url);
        };

        return (
            <div className="usa-radio bg-base-lightest padding-2 border-bottom-1px border-gray-30">
                <input
                    className="usa-radio__input"
                    id={`message-${index}`}
                    type="radio"
                    name="message-test-form"
                    value={body}
                    onChange={handleSelect}
                    checked={selectedOption === body}
                />
                <label className="usa-radio__label margin-top-0" htmlFor={`message-${index}`}>
                    {title}{" "}
                    <Button type="button" unstyled onClick={openTextInNewTab}>
                        View message
                        <Icon name="Visibility" className="text-tbottom margin-left-05" />
                    </Button>
                </label>
            </div>
        );
    };

    return (
        <>
            <Helmet>
                <title>Message testing - ReportStream</title>
            </Helmet>
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
                                <RadioField key={index} index={index} title={item.fileName} body={item.reportBody} />
                            ))}
                            {openCustomMessage && <CustomMessage />}
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
