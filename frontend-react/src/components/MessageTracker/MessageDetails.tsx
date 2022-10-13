import React from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Button } from "@trussworks/react-uswds";

import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { DetailItem } from "../DetailItem/DetailItem";

import { MemberType } from "../../hooks/UseOktaMemberships";
import { ErrorPage } from "../../pages/error/ErrorPage";

type MessageDetailsProps = {
    messageId: string | undefined;
};

const MOCK_MESSAGE_DETAILS = {
    id: 1,
    messageId: "12-234567",
    sender: "somebody 1",
    submittedDate: "09/28/2022",
    reportId: "29038fca-e521-4af8-82ac-6b9fafd0fd58",
    fileUrl: "https://someurl",
};

function MessageDetailsContent() {
    const { messageId } = useParams<MessageDetailsProps>();
    // TODO: call endpoint with messageId to get details
    const messageDetails = MOCK_MESSAGE_DETAILS;

    if (!messageDetails) return <ErrorPage type="page" />;

    return (
        <div className="display-flex flex-column">
            <DetailItem
                item={"Message ID"}
                content={messageDetails.messageId}
            ></DetailItem>
            <h2>Sender:</h2>
            <DetailItem
                item={"Sender Name"}
                content={messageDetails.sender}
            ></DetailItem>
            <DetailItem
                item={"Incoming Report ID"}
                content={messageDetails.reportId}
            ></DetailItem>
            <DetailItem
                item={"Incoming File URL"}
                content={messageDetails.fileUrl}
            ></DetailItem>
        </div>
    );
}

export function MessageDetails() {
    const navigate = useNavigate();

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
                {withCatchAndSuspense(<MessageDetailsContent />)}
            </div>
        </>
    );
}

export const MessageDetailsWithAuth = () => (
    <AuthElement
        element={<MessageDetails />}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
);
