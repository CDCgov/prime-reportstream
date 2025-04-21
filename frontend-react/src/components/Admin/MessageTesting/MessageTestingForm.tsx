import { Button } from "@trussworks/react-uswds";
import { MouseEventHandler, useState } from "react";
import { MessageTestingCustomMessage } from "./MessageTestingCustomMessage";
import { MessageTestingRadioField } from "./MessageTestingRadioField";
import type { RSMessage, RSMessageSender } from "../../../config/endpoints/reports";

export interface RSSubmittedMessage extends Omit<RSMessage, "dateCreated"> {
    dateCreated: Date;
}

interface MessageTestingFormProps {
    currentTestMessages: RSMessage[];
    setCurrentTestMessages: (messages: RSMessage[]) => void;
    handleSubmit: MouseEventHandler<HTMLButtonElement>;
    setSelectedOption: (message: RSMessage | null) => void;
    selectedOption: RSMessage | null;
    senderData: RSMessageSender[];
}

const MessageTestingForm = ({
    currentTestMessages,
    setCurrentTestMessages,
    handleSubmit,
    setSelectedOption,
    selectedOption,
    senderData,
}: MessageTestingFormProps) => {
    const [openCustomMessage, setOpenCustomMessage] = useState(false);

    const [customMessageNumber, setCustomMessageNumber] = useState(1);

    const handleSelect = (item: RSMessage) => {
        setSelectedOption(item);
    };

    const handleAddCustomMessage = () => {
        setSelectedOption(null);
        setOpenCustomMessage(true);
    };

    return (
        <>
            <p className="font-sans-xl text-bold">Test message bank</p>
            <section className="bg-base-lightest padding-3">
                {!currentTestMessages.length && <p>No test messages available</p>}
                {!!currentTestMessages.length && (
                    <form aria-label="Test message form">
                        <fieldset className="usa-fieldset bg-base-lightest padding-3">
                            {currentTestMessages?.map((item, index) => (
                                <MessageTestingRadioField
                                    key={index}
                                    index={index}
                                    title={item.fileName}
                                    body={item.reportBody}
                                    handleSelect={() => handleSelect(item)}
                                    selectedOption={selectedOption ?? null}
                                />
                            ))}
                            {openCustomMessage && (
                                <MessageTestingCustomMessage
                                    customMessageNumber={customMessageNumber}
                                    currentTestMessages={currentTestMessages}
                                    setCustomMessageNumber={setCustomMessageNumber}
                                    setCurrentTestMessages={setCurrentTestMessages}
                                    setOpenCustomMessage={setOpenCustomMessage}
                                    setSelectedOption={setSelectedOption}
                                    senderData={senderData}
                                />
                            )}
                        </fieldset>
                        <div className="padding-top-4">
                            {!openCustomMessage && (
                                <Button type="button" outline onClick={handleAddCustomMessage}>
                                    Add custom message
                                </Button>
                            )}
                        </div>
                    </form>
                )}
            </section>
            <Button className="margin-top-4" disabled={!selectedOption} type="submit" onClick={handleSubmit}>
                Run test
            </Button>
        </>
    );
};

export default MessageTestingForm;
