import { Button } from "@trussworks/react-uswds";
import { ChangeEvent, useState } from "react";
import { MessageTestingCustomMessage } from "./MessageTestingCustomMessage";
import { MessageTestingRadioField } from "./MessageTestingRadioField";
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { warningMessageResult } from "./MessageTestingResult.fixtures";
import type { RSMessage } from "../../../config/endpoints/reports";
import AdminFetchAlert from "../../alerts/AdminFetchAlert";

export interface RSSubmittedMessage extends Omit<RSMessage, "dateCreated"> {
    dateCreated: Date;
}

interface MessageTestingFormProps {
    isDisabled: boolean;
    currentTestMessages: RSMessage[];
    setCurrentTestMessages: (messages: RSMessage[]) => void;
    customMessageNumber: number;
    setCustomMessageNumber: (number: number) => void;
    handleSubmit: () => void;
}

const fakeResultData = warningMessageResult;

const MessageTestingForm = ({
    isDisabled,
    currentTestMessages,
    setCurrentTestMessages,
    customMessageNumber,
    setCustomMessageNumber,
    handleSubmit,
}: MessageTestingFormProps) => {
    // // TODO: Replace with submission hook
    // const [resultData, setResultData] = useState<RSMessageResult | null>(null);
    // const [submittedMessage, setSubmittedMessage] = useState<RSSubmittedMessage | null>(null);

    // const handleSubmit = useCallback<FormEventHandler<HTMLFormElement>>((e) => {
    //     const formData = Object.fromEntries(
    //         new FormData(e.currentTarget).entries(),
    //     ) as unknown as MessageTestingFormValues;

    //     // TODO: Remove fake result data usage, and Submit formData.testMessageBody to server
    //     setSubmittedMessage({
    //         fileName: formData.testMessage,
    //         reportBody: formData.testMessageBody,
    //         dateCreated: new Date(),
    //     });
    //     setResultData(fakeResultData);
    // }, []);

    // if (submittedMessage && resultData) {
    //     return <MessageTestingResult result={resultData} message={submittedMessage} />;
    // }

    const [selectedOption, setSelectedOption] = useState<string | null>(null);
    const [openCustomMessage, setOpenCustomMessage] = useState(false);

    const handleSelect = (event: ChangeEvent<HTMLInputElement>) => {
        setSelectedOption(event.target.value);
    };

    const handleAddCustomMessage = () => {
        setSelectedOption(null);
        setOpenCustomMessage(true);
    };

    if (isDisabled) {
        return <AdminFetchAlert />;
    }

    /**
     * Insert selected message body into hidden field so that parent submit handler has complete form
     */
    // const handleSubmit = useCallback<FormEventHandler<HTMLFormElement>>(
    //     (e) => {
    //         e.preventDefault();

    //         const formData = Object.fromEntries(
    //             new FormData(e.currentTarget).entries(),
    //         ) as MessageTestingFormValuesInternal;
    //         const testMessage = allTestMessages.find((m) => m.fileName === formData.testMessage);

    //         if (testMessage == null) throw new Error("Invalid message");
    //         if (inputRef.current == null) throw new Error("Input ref missing");

    //         inputRef.current.value = testMessage.reportBody;
    //         onSubmit?.(e);
    //     },
    //     [allTestMessages, onSubmit],
    // );

    return (
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
                                handleSelect={handleSelect}
                                selectedOption={selectedOption}
                            />
                        ))}
                        {openCustomMessage && (
                            <MessageTestingCustomMessage
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
                        <Button disabled={!selectedOption} type="submit">
                            Run test
                        </Button>
                    </div>
                </form>
            )}
        </section>
    );
};

export default MessageTestingForm;
