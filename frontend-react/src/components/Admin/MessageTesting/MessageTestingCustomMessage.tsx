import { Button, Select, Textarea } from "@trussworks/react-uswds";
import { ChangeEvent, useState } from "react";
import { RSMessage, RSMessageSender } from "../../../config/endpoints/reports";

export const MessageTestingCustomMessage = ({
    customMessageNumber,
    currentTestMessages,
    setCustomMessageNumber,
    setCurrentTestMessages,
    setOpenCustomMessage,
    setSelectedOption,
    senderData,
}: {
    customMessageNumber: number;
    currentTestMessages: RSMessage[];
    setCustomMessageNumber: (value: number) => void;
    setCurrentTestMessages: (messages: RSMessage[]) => void;
    setOpenCustomMessage: (value: boolean) => void;
    setSelectedOption: (message: RSMessage) => void;
    senderData: RSMessageSender[];
}) => {
    const [text, setText] = useState("");
    const [senderId, setSenderId] = useState("");
    const handleTextareaChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
        setText(event.target.value);
    };
    const handleAddCustomMessage = () => {
        const dateCreated = new Date();
        const customTestMessage = {
            dateCreated: dateCreated.toString(),
            fileName: `Custom message ${customMessageNumber}`,
            reportBody: text,
            senderId: senderId,
        };
        setCurrentTestMessages([...currentTestMessages, customTestMessage]);
        setCustomMessageNumber(customMessageNumber + 1);
        setText("");
        setSelectedOption(customTestMessage);
        setOpenCustomMessage(false);
    };

    return (
        <div className="width-full">
            <p className="text-bold">Enter custom message (HL7 or FHIR)</p>
            <p>Custom messages do not save to the bank after you log out.</p>
            <Textarea
                value={text}
                onChange={handleTextareaChange}
                id="custom-message-text"
                name="custom-message-text"
                aria-label="Custom message text"
                className="width-full maxw-full margin-bottom-205"
            />
            <p className="text-bold">Select sender</p>
            <p>Selecting a sender will apply the sender’s transform</p>
            {senderData ? (
                <Select
                    id="sender-dropdown"
                    name="sender-dropdown"
                    onChange={(e) => {
                        setSenderId(e.target.value);
                    }}
                >
                    <option hidden>-Select-</option>
                    {senderData.map((item, index) => (
                        <option key={index} value={item.id}>
                            {item.id}
                        </option>
                    ))}
                </Select>
            ) : (
                <p className="text-italic">Unable to load sender data</p>
            )}

            <div className="width-full margin-top-3">
                <Button
                    type="button"
                    outline
                    onClick={() => {
                        setOpenCustomMessage(false);
                    }}
                >
                    Cancel
                </Button>
                <Button type="button" onClick={handleAddCustomMessage} disabled={text.length === 0 || senderId === ""}>
                    Add custom message
                </Button>
            </div>
        </div>
    );
};
