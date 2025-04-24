import { Button, Textarea } from "@trussworks/react-uswds";
import { ChangeEvent, useState } from "react";
import { RSMessage } from "../../../config/endpoints/reports";

export const MessageTestingCustomMessage = ({
    customMessageNumber,
    currentTestMessages,
    setCustomMessageNumber,
    setCurrentTestMessages,
    setOpenCustomMessage,
    setSelectedOption,
}: {
    customMessageNumber: number;
    currentTestMessages: RSMessage[];
    setCustomMessageNumber: (value: number) => void;
    setCurrentTestMessages: (messages: RSMessage[]) => void;
    setOpenCustomMessage: (value: boolean) => void;
    setSelectedOption: (message: RSMessage) => void;
}) => {
    const [text, setText] = useState("");
    const handleTextareaChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
        setText(event.target.value);
    };
    const handleAddCustomMessage = () => {
        const dateCreated = new Date();
        const customTestMessage = {
            dateCreated: dateCreated.toString(),
            fileName: `Custom message ${customMessageNumber}`,
            reportBody: text,
            senderId: "None",
        };
        setCurrentTestMessages([...currentTestMessages, customTestMessage]);
        setCustomMessageNumber(customMessageNumber + 1);
        setText("");
        setSelectedOption(customTestMessage);
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
                aria-label="Custom message text"
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
