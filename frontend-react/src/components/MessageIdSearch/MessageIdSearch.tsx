import React, { FormEvent, useState } from "react";
import { Label, Search } from "@trussworks/react-uswds";

import { withCatchAndSuspense } from "../RSErrorBoundary";
import Spinner from "../Spinner";
import Table, { TableConfig } from "../../components/Table/Table";

const MOCK_MESSAGE_ID_DATA = [
    {
        id: 1,
        messageId: "12-234567",
        sender: "somebody 1",
        submittedDate: "09/28/2022",
        reportId: "29038fca-e521-4af8-82ac-6b9fafd0fd58",
    },
    {
        id: 2,
        messageId: "12-234567",
        sender: "somebody 2",
        submittedDate: "09/29/2022",
        reportId: "86c4c66f-3d99-4845-8bea-111210c05e63",
    },
    {
        id: 3,
        messageId: "12-234567",
        sender: "somebody 3",
        submittedDate: "09/30/2022",
        reportId: "26a37945-ed12-4578-b4f6-203e8b9d62ce",
    },
];

// TODO: move this interface into the resources directory
interface MessageListResource {
    messageId: string;
    sender: string;
    submittedDate: string;
    reportId: string;
}

interface MessageListTableContentProps {
    isLoading: boolean;
    messageIdData: MessageListResource[];
}

const MessageIdTableContent: React.FC<MessageListTableContentProps> = ({
    isLoading,
    messageIdData,
}) => {
    const tableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "messageId",
                columnHeader: "Message Id",
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
        rows: messageIdData || [],
    };

    if (isLoading) return <Spinner />;

    return (
        <>
            {messageIdData.length > 0 && (
                <Table
                    title="ReportStream Messages"
                    classes={"rs-no-padding"}
                    config={tableConfig}
                />
            )}
        </>
    );
};

// Main component.
export function MessageIdSearch() {
    const [isLoading, setIsLoading] = useState(false);
    const [messagesData, setMessagesData] = useState<MessageListResource[]>([]);

    const searchMessageId = (event: FormEvent<HTMLFormElement>) => {
        // Prevent page refresh
        event.preventDefault();

        setIsLoading(true);
        // TODO: make API call here to get data and remove timeout
        setTimeout(function () {
            setMessagesData(MOCK_MESSAGE_ID_DATA);
            setIsLoading(false);
        }, 1000);
    };

    return (
        <section className="grid-container margin-bottom-5 tablet:margin-top-6">
            <h1>Message ID Search</h1>

            <div>
                <Label
                    className="font-sans-xs usa-label"
                    htmlFor="input_filter"
                >
                    Message ID (format as xx-xxxxxx)
                </Label>
                <Search
                    id="search_filter"
                    name="search_filter"
                    inputMode="text"
                    aria-disabled={isLoading}
                    onSubmit={searchMessageId}
                />
            </div>

            {withCatchAndSuspense(
                <MessageIdTableContent
                    isLoading={isLoading}
                    messageIdData={messagesData}
                ></MessageIdTableContent>
            )}
        </section>
    );
}
