import { Button, Radio } from "@trussworks/react-uswds";
import { type ComponentPropsWithoutRef, type FormEventHandler, useCallback, useMemo, useRef, useState } from "react";
import AddCustomMessageForm, { type AddCustomMessageFormValues } from "./AddCustomTestMessageForm";
import TestMessageLabel from "./TestMessageLabel";
import type { RSMessage } from "../../../config/endpoints/reports";

export interface MessageTestingFormBaseProps extends ComponentPropsWithoutRef<"form"> {
    id: string;
    testMessages: RSMessage[];
}

export interface MessageTestingFormValuesInternal {
    testMessage?: string;
    testMessageBody?: string;
}

export interface MessageTestingFormValues {
    testMessage: string;
    testMessageBody: string;
}

/**
 * AKA Report Testing
 * Form holds selected test message name and body (hidden).
 */
const MessageTestingFormBase = ({ testMessages, onChange, onSubmit, id, ...props }: MessageTestingFormBaseProps) => {
    const [customTestMessages, setCustomTestMessages] = useState<RSMessage[]>([]);
    const allTestMessages = useMemo(() => [...testMessages, ...customTestMessages], [customTestMessages, testMessages]);

    const inputRef = useRef<HTMLInputElement>(null);

    const [isCustomMessageFormOpen, setIsCustomMessageFormOpen] = useState(false);
    const [isSubmitEnabled, setIsSubmitEnabled] = useState(false);

    const handleOpenCustomMessageForm = useCallback(() => {
        setIsCustomMessageFormOpen(true);
    }, [setIsCustomMessageFormOpen]);

    const handleAddCustomMessageCancel = useCallback(() => {
        setIsCustomMessageFormOpen(false);
    }, [setIsCustomMessageFormOpen]);

    const handleChange = useCallback<FormEventHandler<HTMLFormElement>>(
        (e) => {
            setIsSubmitEnabled(e.currentTarget.checkValidity());
            onChange?.(e);
        },
        [onChange],
    );

    /**
     * Insert selected message body into hidden field so that parent submit handler has complete form
     */
    const handleSubmit = useCallback<FormEventHandler<HTMLFormElement>>(
        (e) => {
            e.preventDefault();

            const formData = Object.fromEntries(
                new FormData(e.currentTarget).entries(),
            ) as MessageTestingFormValuesInternal;
            const testMessage = allTestMessages.find((m) => m.fileName === formData.testMessage);

            if (testMessage == null) throw new Error("Invalid message");
            if (inputRef.current == null) throw new Error("Input ref missing");

            inputRef.current.value = testMessage.reportBody;
            onSubmit?.(e);
        },
        [allTestMessages, onSubmit],
    );

    const handleAddCustomMessageSubmit = useCallback<FormEventHandler<HTMLFormElement>>(
        (e) => {
            e.preventDefault();

            const formData = Object.fromEntries(
                new FormData(e.currentTarget).entries(),
            ) as unknown as AddCustomMessageFormValues;
            const dateCreated = new Date();
            const customTestMessage = {
                dateCreated: dateCreated.toString(),
                fileName: `Custom message ${customTestMessages.length + 1}`,
                reportBody: formData.customMessageTestBody,
            };
            setCustomTestMessages((m) => [...m, customTestMessage]);
            setIsCustomMessageFormOpen(false);
        },
        [customTestMessages],
    );

    return (
        <>
            <section className="bg-base-lightest padding-3">
                {!allTestMessages.length && <p>No test messages available</p>}
                {!!allTestMessages.length && (
                    <form {...props} id={id} onChange={handleChange} onSubmit={handleSubmit}>
                        <fieldset className="usa-fieldset">
                            {allTestMessages?.map((item, index) => (
                                <Radio
                                    id={`message-${index}`}
                                    name="testMessage"
                                    value={item.fileName}
                                    className="usa-radio bg-base-lightest padding-2 border-bottom-1px border-gray-30"
                                    label={<TestMessageLabel data={item.reportBody}> {item.fileName}</TestMessageLabel>}
                                    key={item.fileName}
                                    title={item.fileName}
                                    required={true}
                                />
                            ))}
                            <input name="testMessageBody" type="hidden" ref={inputRef} />
                        </fieldset>
                    </form>
                )}
                {isCustomMessageFormOpen && (
                    <AddCustomMessageForm
                        onCancel={handleAddCustomMessageCancel}
                        onSubmit={handleAddCustomMessageSubmit}
                    />
                )}
            </section>
            <div className="padding-top-4">
                <Button type="button" outline onClick={handleOpenCustomMessageForm}>
                    Test custom message
                </Button>
                <Button disabled={!isSubmitEnabled} type="submit" form={id}>
                    Run test
                </Button>
            </div>
        </>
    );
};

export default MessageTestingFormBase;
