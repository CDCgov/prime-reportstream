import { Button } from "@trussworks/react-uswds";
import { useState } from "react";
import { MessageTestingCustomMessage } from "./MessageTestingCustomMessage";
import { MessageTestingRadioField } from "./MessageTestingRadioField";
import type { RSMessage } from "../../../config/endpoints/reports";

export interface RSSubmittedMessage extends Omit<RSMessage, "dateCreated"> {
    dateCreated: Date;
}

interface MessageTestingFormProps {
    currentTestMessages: RSMessage[];
    setCurrentTestMessages: (messages: RSMessage[]) => void;
    handleSubmit: (e: React.FormEvent<HTMLFormElement>) => void;
    setSelectedOption: (message: RSMessage | null) => void;
    selectedOption: RSMessage | null;
}

const MessageTestingForm = ({
    currentTestMessages,
    setCurrentTestMessages,
    handleSubmit,
    setSelectedOption,
    selectedOption,
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
                    <form onSubmit={handleSubmit}>
                        <fieldset className="usa-fieldset bg-base-lightest padding-3">
                            {currentTestMessages?.map((item, index) => (
                                <MessageTestingRadioField
                                    key={index}
                                    index={index}
                                    title={item.fileName}
                                    body={item.reportBody}
                                    handleSelect={() => handleSelect(item)}
                                    selectedOption={selectedOption?.reportBody ? selectedOption.reportBody : null}
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
                                />
                            )}
                        </fieldset>
                        <div className="padding-top-4">
                            <Button type="button" outline onClick={handleAddCustomMessage}>
                                Test custom message
                            </Button>
                            <Button disabled={!selectedOption} type="submit">
                                Run test
                            </Button>
                        </div>
                    </form>
                )}
            </section>
        </>
    );
};

export default MessageTestingForm;
