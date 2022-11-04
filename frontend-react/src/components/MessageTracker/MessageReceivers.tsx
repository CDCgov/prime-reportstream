import React from "react";

import { DetailItem } from "../DetailItem/DetailItem";
import { ReceiverData } from "../../config/endpoints/messageTracker";
import { formattedDateFromTimestamp } from "../../utils/DateTimeUtils";

import { QualityFilters } from "./QualityFilters";

type MessageReceiverProps = {
    receiverDetails: ReceiverData[] | undefined;
};

export const MessageReceivers = ({ receiverDetails }: MessageReceiverProps) => {
    return (
        <>
            <h3>Receivers:</h3>
            {receiverDetails?.map((receiver, i) => (
                <div
                    className="display-flex flex-column margin-bottom-5"
                    key={"receiver_" + i}
                >
                    <DetailItem
                        item={"Receiver Name"}
                        content={receiver.receivingOrg}
                    ></DetailItem>
                    <DetailItem
                        item={"Receiver Service"}
                        content={receiver.receivingOrgSvc}
                    ></DetailItem>
                    <DetailItem
                        item={"Incoming Report Id"}
                        content={receiver.reportId}
                    ></DetailItem>
                    <DetailItem
                        item={"Incoming File Name"}
                        content={receiver.fileName}
                    ></DetailItem>
                    <DetailItem
                        item={"Incoming File URL"}
                        content={receiver.fileUrl}
                    ></DetailItem>
                    <DetailItem
                        item={"Date/Time Submitted"}
                        content={formattedDateFromTimestamp(
                            receiver.createdAt,
                            "MMMM DD YYYY"
                        )}
                    ></DetailItem>
                    <DetailItem
                        item={"Transport Results"}
                        content={receiver.transportResult}
                    ></DetailItem>
                    {receiver.qualityFilters.length > 0 && (
                        <QualityFilters
                            qualityFilters={receiver.qualityFilters}
                        />
                    )}
                </div>
            ))}
        </>
    );
};
