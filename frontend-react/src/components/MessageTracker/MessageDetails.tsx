import React from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Button } from "@trussworks/react-uswds";

import { AuthElement } from "../AuthElement";
import { DetailItem } from "../DetailItem/DetailItem";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { useMessageDetails } from "../../hooks/network/MessageTracker/MessageTrackerHooks";

import { WarningsErrors } from "./WarningsErrors";
import { MessageReceivers } from "./MessageReceivers";

type MessageDetailsProps = {
    id: string | undefined;
};

export function MessageDetails() {
    const navigate = useNavigate();
    const { id } = useParams<MessageDetailsProps>();
    const { messageDetails } = useMessageDetails(id!!);

    return (
        <>
            <div
                className="grid-container margin-bottom-10"
                data-testid="container"
            >
                <div className="margin-bottom-5">
                    <h1>Message Details</h1>
                    <Button onClick={() => navigate(-1)} type="button">
                        Back
                    </Button>
                </div>
                <DetailItem
                    item={"Message ID"}
                    content={messageDetails!.messageId}
                ></DetailItem>
                <div className="display-flex flex-column margin-bottom-5">
                    <h3>Sender:</h3>
                    <DetailItem
                        item={"Sender Name"}
                        content={messageDetails!.sender}
                    ></DetailItem>
                    <DetailItem
                        item={"Incoming Report ID"}
                        content={messageDetails!.reportId}
                    ></DetailItem>
                    <DetailItem
                        item={"Incoming File Name"}
                        content={messageDetails!.fileName}
                    ></DetailItem>
                    <DetailItem
                        item={"Incoming File URL"}
                        content={messageDetails!.fileUrl}
                    ></DetailItem>
                </div>
                <MessageReceivers
                    receiverDetails={messageDetails!.receiverData}
                />
                <WarningsErrors
                    title={"Warnings:"}
                    data={messageDetails!.warnings.map((md) => md.detail)}
                />
                <WarningsErrors
                    title={"Errors:"}
                    data={messageDetails!.errors.map((md) => md.detail)}
                />
            </div>
        </>
    );
}

export const MessageDetailsWithAuth = () => (
    <AuthElement
        element={withCatchAndSuspense(<MessageDetails />)}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
);
