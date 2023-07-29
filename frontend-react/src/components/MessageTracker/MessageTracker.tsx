import React, { useState } from "react";
import {
    Form,
    FormGroup,
    Label,
    Button,
    TextInput,
    ButtonGroup,
    Grid,
} from "@trussworks/react-uswds";

import Spinner from "../Spinner";
import { MessageListResource } from "../../config/endpoints/messageTracker";
import { useMessageSearch } from "../../hooks/network/MessageTracker/MessageTrackerHooks";
import { Table } from "../../shared/Table/Table";
import { USLink } from "../USLink";

interface MessageListTableContentProps {
    isLoading: boolean;
    messagesData: MessageListResource[];
    hasSearched: boolean;
}

const MessageTrackerTableContent: React.FC<MessageListTableContentProps> = ({
    isLoading,
    messagesData,
    hasSearched,
}) => {
    if (isLoading) return <Spinner />;

    const formattedTableData = messagesData.map((row) => {
        return [
            {
                columnKey: "messageId",
                columnHeader: "Message ID",
                content: (
                    <USLink href={`/message-details/${row.id}`}>
                        {row.messageId}
                    </USLink>
                ),
            },
            {
                columnKey: "sender",
                columnHeader: "Sender",
                content: row.sender,
            },
            {
                columnKey: "submittedDate",
                columnHeader: "Date/time submitted",
                content: row.submittedDate
                    ? new Date(row.submittedDate).toLocaleString()
                    : "",
            },
            {
                columnKey: "reportId",
                columnHeader: "Incoming Report Id",
                content: (
                    <USLink href={`/submissions/${row.reportId}`}>
                        {row.reportId}
                    </USLink>
                ),
            },
        ];
    });

    return (
        <>
            {hasSearched && (
                <Table borderless striped rowData={formattedTableData} />
            )}
        </>
    );
};

// Main component.
export function MessageTracker() {
    const [searchFilter, setSearchFilter] = useState("");
    const [messagesData, setMessagesData] = useState<MessageListResource[]>([]);
    const [hasSearched, setHasSearched] = useState(false);
    const { search, isLoading } = useMessageSearch();

    const searchMessageId = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const senderResponse: MessageListResource[] = await search(
            searchFilter
        );

        setHasSearched(true);

        setMessagesData(senderResponse);
    };

    const clearSearch = () => {
        setSearchFilter("");
        setMessagesData([]);
    };

    return (
        <section className="margin-bottom-5 tablet:margin-top-6">
            <h1>Message ID Search</h1>

            <Form onSubmit={(e) => searchMessageId(e)} className="maxw-full">
                <Grid row gap="lg" className="flex-align-end">
                    <Grid col={6}>
                        <FormGroup>
                            <Label
                                className="font-sans-xs usa-label"
                                htmlFor="search-field"
                            >
                                Message ID
                            </Label>
                            <TextInput
                                id="search-field"
                                name="search"
                                type="text"
                                autoFocus
                                inputSize={"medium"}
                                aria-disabled={isLoading}
                                value={searchFilter}
                                onChange={(evt) =>
                                    setSearchFilter(
                                        (
                                            evt.target as HTMLInputElement
                                        ).value.trim()
                                    )
                                }
                                required={true}
                            />
                        </FormGroup>
                    </Grid>
                    <Grid col="auto">
                        <ButtonGroup>
                            <Button
                                type="submit"
                                name="submit-button"
                                className="usa-button radius-left-1"
                            >
                                Search
                            </Button>
                            <Button
                                onClick={() => clearSearch()}
                                type="button"
                                name="clear-button"
                                className="font-sans-xs"
                                outline
                            >
                                Clear
                            </Button>
                        </ButtonGroup>
                    </Grid>
                </Grid>
            </Form>

            <MessageTrackerTableContent
                isLoading={isLoading}
                messagesData={messagesData || []}
                hasSearched={hasSearched}
            ></MessageTrackerTableContent>
        </section>
    );
}
