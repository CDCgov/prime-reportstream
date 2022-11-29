import React, { useState } from "react";
import {
    Form,
    FormGroup,
    Label,
    Button,
    TextInput,
} from "@trussworks/react-uswds";

import Spinner from "../Spinner";
import Table, { TableConfig } from "../../components/Table/Table";
import { MessageListResource } from "../../config/endpoints/messageTracker";
import { useMessageSearch } from "../../hooks/network/MessageTracker/MessageTrackerHooks";

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
    const tableConfig: TableConfig = {
        columns: [
            {
                dataAttr: "messageId",
                columnHeader: "Message Id",
                feature: {
                    link: true,
                    linkAttr: "id",
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
            {hasSearched && (
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
        <section className="grid-container margin-bottom-5 tablet:margin-top-6">
            <h1>Message ID Search</h1>

            <Form onSubmit={(e) => searchMessageId(e)} className="maxw-full">
                <div className="grid-row display-flex">
                    <div className="display-flex">
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
                                className={
                                    "usa-input rs-max-width-100-important mobile:width-card-lg mobile-lg:width-mobile tablet:width-tablet"
                                }
                                autoFocus
                                inputSize={"medium"}
                                aria-disabled={isLoading}
                                value={searchFilter}
                                onChange={(evt) =>
                                    setSearchFilter(
                                        (evt.target as HTMLInputElement).value
                                    )
                                }
                                required={true}
                            />
                        </FormGroup>
                        <Button
                            type="submit"
                            name="submit-button"
                            className="usa-button height-5 radius-left-0 rs-margin-top-auto-important margin-right-3"
                        >
                            Search
                        </Button>
                    </div>
                    <FormGroup className="display-flex">
                        <Button
                            onClick={() => clearSearch()}
                            type="button"
                            name="clear-button"
                            className="font-sans-xs"
                            unstyled
                        >
                            Clear
                        </Button>
                    </FormGroup>
                </div>
            </Form>

            <MessageTrackerTableContent
                isLoading={isLoading}
                messagesData={messagesData || []}
                hasSearched={hasSearched}
            ></MessageTrackerTableContent>
        </section>
    );
}
