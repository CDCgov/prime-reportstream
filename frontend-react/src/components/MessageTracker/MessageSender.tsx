import React from "react";

import { DetailItem } from "../DetailItem/DetailItem";

// TODO: move this interface into the resources directory
interface ResponseSender {
    id: number;
    messageId: string;
    sender: string | undefined;
    submittedDate: string | undefined;
    reportId: string;
    fileUrl: string | undefined;
}
type MessageSenderProps = {
    senderDetails: ResponseSender;
};

export const MessageSender = ({ senderDetails }: MessageSenderProps) => {
    return (
        <div className="display-flex flex-column margin-bottom-5">
            <DetailItem
                item={"Message ID"}
                content={senderDetails.messageId}
            ></DetailItem>
            <h3>Sender:</h3>
            <DetailItem
                item={"Sender Name"}
                content={senderDetails.sender}
            ></DetailItem>
            <DetailItem
                item={"Incoming Report ID"}
                content={senderDetails.reportId}
            ></DetailItem>
            <DetailItem
                item={"Incoming File URL"}
                content={senderDetails.fileUrl}
            ></DetailItem>
        </div>
    );
};
