import React from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Accordion, GridContainer } from "@trussworks/react-uswds";
import { AccordionItemProps } from "@trussworks/react-uswds/lib/components/Accordion/Accordion";

import { AuthElement } from "../AuthElement";
import { DetailItem } from "../DetailItem/DetailItem";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { useMessageDetails } from "../../hooks/network/MessageTracker/MessageTrackerHooks";
import { WarningError } from "../../config/endpoints/messageTracker";
import { parseFileLocation } from "../../utils/misc";

import { WarningsErrors } from "./WarningsErrors";
import { MessageReceivers } from "./MessageReceivers";

type MessageDetailsProps = {
    id: string | undefined;
};

const dateTimeFormatter = new Intl.DateTimeFormat("en-US", {
    month: "2-digit",
    day: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
});

const dataToAccordionItems = (props: {
    id: string;
    title: string;
    data: WarningError[];
}): AccordionItemProps[] => {
    const results: AccordionItemProps[] = [];
    if (props.data.length === 0) {
        return [];
    }
    results.push({
        id: props.id,
        title: props.title,
        expanded: false,
        content: (
            <WarningsErrors
                title={""}
                data={props.data.map((md) => md.detail)}
            />
        ),
        headingLevel: "h3",
        className: "",
    });

    return results;
};

export function MessageDetails() {
    const navigate = useNavigate();
    const { id } = useParams<MessageDetailsProps>();
    const { messageDetails } = useMessageDetails(id!!);
    const submittedDate = messageDetails?.submittedDate
        ? new Date(messageDetails.submittedDate)
        : undefined;
    const warnings = messageDetails?.warnings || [];
    const errors = messageDetails?.errors || [];

    const fileUrl = messageDetails?.fileUrl || "";
    const { folderLocation, sendingOrg, fileName } = parseFileLocation(fileUrl);

    return (
        <GridContainer>
            <article className="margin-bottom-10" data-testid="container">
                <div>
                    <h2>Message ID Search</h2>
                    <Button
                        onClick={() => navigate("/admin/message-tracker")}
                        type="button"
                    >
                        Back
                    </Button>
                </div>
                <hr className="margin-top-4 margin-bottom-4" />
                <div className="display-flex flex-column">
                    <span className="text-base font-sans-sm margin-bottom-1">
                        Message ID
                    </span>
                    <h1>{messageDetails!.messageId}</h1>
                </div>
                <hr />
                <div className="display-flex flex-column margin-bottom-7">
                    <h2 className="font-sans-lg margin-bottom-4">Sender:</h2>
                    <div className="font-sans-sm bg-gray-5 radius-lg padding-top-3 padding-bottom-2 padding-left-3 padding-right-3">
                        <div
                            className={
                                warnings.length || errors.length
                                    ? "margin-bottom-4"
                                    : ""
                            }
                        >
                            <DetailItem
                                item="Submitter"
                                content={messageDetails!.sender}
                            />
                            <DetailItem
                                item="Incoming Report ID"
                                content={messageDetails!.reportId}
                            />
                            <DetailItem
                                item="Date/Time Submitted"
                                content={dateTimeFormatter.format(
                                    submittedDate,
                                )}
                            />
                            <div className="display-flex flex-column margin-bottom-2">
                                <span className="text-base line-height-body-5">
                                    {"File Location"}
                                </span>
                                <div>
                                    {folderLocation && sendingOrg && (
                                        <>
                                            <span className="font-mono-sm border-1px bg-primary-lighter radius-md padding-top-4px padding-bottom-4px padding-left-1 padding-right-1">
                                                {folderLocation.toUpperCase()}
                                            </span>
                                            <span className="padding-x-1">
                                                /
                                            </span>
                                            <span>{`${sendingOrg}`}</span>
                                        </>
                                    )}
                                </div>
                            </div>
                            <DetailItem
                                item="Incoming File Name"
                                content={fileName}
                            />
                        </div>

                        <div
                            className={
                                warnings.length
                                    ? "message-tracker display-flex flex-column margin-bottom-2"
                                    : ""
                            }
                        >
                            <Accordion
                                bordered={true}
                                multiselectable={true}
                                className="bg-primary"
                                items={dataToAccordionItems({
                                    id: "accord-item-warnings",
                                    title: `Warnings (${
                                        messageDetails!.warnings.length
                                    })`,
                                    data: warnings,
                                })}
                            />
                        </div>

                        <div
                            className={
                                errors.length
                                    ? "message-tracker display-flex flex-column margin-bottom-2"
                                    : ""
                            }
                        >
                            <Accordion
                                bordered={true}
                                multiselectable={true}
                                items={dataToAccordionItems({
                                    id: "accord-item-errors",
                                    title: `Errors (${
                                        messageDetails!.errors.length
                                    })`,
                                    data: errors,
                                })}
                            />
                        </div>
                    </div>
                </div>
                <hr className="margin-top-2 margin-bottom-4" />
                {messageDetails?.receiverData! ? (
                    <MessageReceivers
                        receiverDetails={messageDetails.receiverData}
                    />
                ) : (
                    <>
                        <h2>Receivers:</h2>
                        <h3>No Data to Display</h3>
                    </>
                )}
            </article>
        </GridContainer>
    );
}

export const MessageDetailsWithAuth = () => (
    <AuthElement
        element={withCatchAndSuspense(<MessageDetails />)}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
);
