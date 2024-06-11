import {
    Button,
    Form,
    FormGroup,
    Label,
    TextInput,
} from "@trussworks/react-uswds";
import { FC, FormEvent, useState } from "react";

import { MessageListResource } from "../../config/endpoints/messageTracker";
import useMessageSearch from "../../hooks/api/messages/UseMessageSearch/UseMessageSearch";
import Table from "../../shared/Table/Table";
import Spinner from "../Spinner";
import { USLink } from "../USLink";

interface MessageListTableContentProps {
    isLoading: boolean;
    messagesData: MessageListResource[];
    hasSearched: boolean;
}

const MessageTrackerTableContent: FC<MessageListTableContentProps> = ({
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
    const { mutateAsync: search, isPending } = useMessageSearch();

    const searchMessageId = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        const senderResponse: MessageListResource[] =
            await search(searchFilter);

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

            <Form
                onSubmit={(e) => void searchMessageId(e)}
                className="maxw-full"
            >
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
                                inputSize={"medium"}
                                aria-disabled={isPending}
                                value={searchFilter}
                                onChange={(evt) =>
                                    setSearchFilter(
                                        (
                                            evt.target as HTMLInputElement
                                        ).value.trim(),
                                    )
                                }
                                required={true}
                            />
                        </FormGroup>
                        <Button
                            type="submit"
                            name="submit-button"
                            className="usa-button height-5 radius-left-1 rs-margin-top-auto-important margin-right-3 margin-left-3"
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
                isLoading={isPending}
                messagesData={messagesData || []}
                hasSearched={hasSearched}
            ></MessageTrackerTableContent>
        </section>
    );
}
