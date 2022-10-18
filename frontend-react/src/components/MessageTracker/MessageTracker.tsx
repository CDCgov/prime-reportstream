import React, { useState } from "react";
import { Label, Button, TextInput } from "@trussworks/react-uswds";

import Spinner from "../Spinner";
import Table, { TableConfig } from "../../components/Table/Table";

const MOCK_MESSAGE_SENDER_DATA = [
    {
        messageId: "12-234567",
        sender: "somebody 1",
        submittedDate: "09/28/2022",
        reportId: "29038fca-e521-4af8-82ac-6b9fafd0fd58",
    },
    {
        messageId: "12-234567",
        sender: "somebody 2",
        submittedDate: "09/29/2022",
        reportId: "86c4c66f-3d99-4845-8bea-111210c05e63",
    },
    {
        messageId: "12-234567",
        sender: "somebody 3",
        submittedDate: "09/30/2022",
        reportId: "26a37945-ed12-4578-b4f6-203e8b9d62ce",
    },
];

// TODO: move this interface into the resources directory
export interface MessageListResource {
    messageId: string;
    sender: string | undefined;
    submittedDate: string | undefined;
    reportId: string;
}

interface MessageListTableContentProps {
    isLoading: boolean;
    messagesData: MessageListResource[];
}

const MessageTrackerTableContent: React.FC<MessageListTableContentProps> = ({
    isLoading,
    messagesData,
}) => {
    const tableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "messageId",
                columnHeader: "Message Id",
                feature: {
                    link: true,
                    linkBasePath: "/message-details/",
                },
            },
            {
                dataAttr: "sender",
                columnHeader: "Sender",
            },
            {
                dataAttr: "submittedDate",
                columnHeader: "Date/time submitted",
                transform: (s: string) => {
                    return new Date(s).toLocaleString();
                },
            },
            {
                dataAttr: "reportId",
                columnHeader: "Incoming Report Id",
                feature: {
                    link: true,
                    linkBasePath: "/report-details?reportId=",
                },
            },
        ],
        rows: messagesData || [],
    };

    if (isLoading) return <Spinner />;

    return (
        <>
            {messagesData.length > 0 && (
                <Table
                    title=""
                    classes={"rs-no-padding margin-top-5"}
                    config={tableConfig}
                />
            )}
        </>
    );
};

// Main component.
export function MessageTracker() {
    const [searchFilter, setSearchFilter] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [messagesData, setMessagesData] = useState<MessageListResource[]>([]);

    const searchMessageId = async () => {
        setIsLoading(true);

        // TODO: make API call here to get data
        const senderResponse: MessageListResource[] = MOCK_MESSAGE_SENDER_DATA;
        if (senderResponse.length) {
            setMessagesData(senderResponse);
        }

        setIsLoading(false);
    };

    const clearSearch = () => {
        setSearchFilter("");
        setMessagesData([]);
    };

    return (
        <section className="grid-container margin-bottom-5 tablet:margin-top-6">
            <h1>Message ID Search</h1>

            <Label className="font-sans-xs usa-label" htmlFor="input_filter">
                Message ID (format as xx-xxxxxx)
            </Label>
            <div className="grid-gap-lg display-flex">
                <TextInput
                    id="search-field"
                    name="search"
                    type="text"
                    className={"usa-input"}
                    autoFocus
                    inputSize={"medium"}
                    aria-disabled={isLoading}
                    value={searchFilter}
                    onChange={(evt) =>
                        setSearchFilter((evt.target as HTMLInputElement).value)
                    }
                />
                <Button
                    onClick={() => searchMessageId()}
                    type="button"
                    name="submit-button"
                    className="usa-button height-5 margin-top-1 radius-left-0"
                >
                    Search
                </Button>
                <Button
                    onClick={() => clearSearch()}
                    type="button"
                    name="clear-button"
                    className="font-sans-xs margin-top-1"
                    unstyled
                >
                    Clear
                </Button>
            </div>

            <MessageTrackerTableContent
                isLoading={isLoading}
                messagesData={messagesData || []}
            ></MessageTrackerTableContent>
        </section>
    );
}
