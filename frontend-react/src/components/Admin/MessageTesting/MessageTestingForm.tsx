import { FormEventHandler, useCallback, useState } from "react";
import MessageTestingFormBase, {
    type MessageTestingFormBaseProps,
    type MessageTestingFormValues,
} from "./MessageTestingFormBase";
import MessageTestingResult from "./MessageTestingResult";
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { errorMessageResult, passMessageResult, warningMessageResult } from "./MessageTestingResult.fixtures";
import type { RSMessage, RSMessageResult } from "../../../config/endpoints/reports";
import useTestMessages from "../../../hooks/api/messages/UseTestMessages/UseTestMessages";
import AdminFetchAlert from "../../alerts/AdminFetchAlert";

export interface MessageTestingFormProps extends Omit<MessageTestingFormBaseProps, "testMessages"> {}

export interface RSSubmittedMessage extends Omit<RSMessage, "dateCreated"> {
    dateCreated: Date;
}

const fakeResultData = warningMessageResult;

/**
 * Data fetching wrapper for {@link MessageTestingFormBase}
 * @see {@link MessageTestingFormBase}
 */
const MessageTestingForm = () => {
    const { data, isDisabled } = useTestMessages();
    // TODO: Replace with submission hook
    const [resultData, setResultData] = useState<RSMessageResult | null>(null);
    const [submittedMessage, setSubmittedMessage] = useState<RSSubmittedMessage | null>(null);

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
    }, []);

    if (isDisabled) {
        return <AdminFetchAlert />;
    }

    if (submittedMessage && resultData) {
        return <MessageTestingResult result={resultData} message={submittedMessage} />;
    }

    return <MessageTestingFormBase testMessages={data} onSubmit={handleSubmit} id="test-message-form" />;
};

export default MessageTestingForm;
