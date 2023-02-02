import React from "react";
import moment from "moment";
import { useNavigate, useParams } from "react-router-dom";
import { Button, Accordion } from "@trussworks/react-uswds";
import { AccordionItemProps } from "@trussworks/react-uswds/lib/components/Accordion/Accordion";

import { AuthElement } from "../AuthElement";
import { DetailItem } from "../DetailItem/DetailItem";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { useMessageDetails } from "../../hooks/network/MessageTracker/MessageTrackerHooks";
import { WarningError } from "../../config/endpoints/messageTracker";

import { WarningsErrors } from "./WarningsErrors";
import { MessageReceivers } from "./MessageReceivers";

type MessageDetailsProps = {
    id: string | undefined;
};
const parseFileLocation = (
    fileLocation: string
): {
    fileLocation: string;
    sendingOrg: string;
    fileName: string;
} => {
    let fileLocationFromStr = "";
    let sendingOrgFromStr = "";
    let fileNameFromStr = "";

    let fileReportsLocation = fileLocation.split("reports/").pop();
    if (fileReportsLocation) {
        fileLocationFromStr = fileReportsLocation.split("%2F")[0].toUpperCase();
        sendingOrgFromStr = fileReportsLocation.split("%2F")[1];
        fileNameFromStr = fileReportsLocation.split("%2F")[2];
    }

    return {
        fileLocation: fileLocationFromStr,
        sendingOrg: sendingOrgFromStr,
        fileName: fileNameFromStr,
    };
};

const dataToAccordionItems = (props: {
    id: string;
    title: string;
    data: WarningError[];
}): AccordionItemProps[] => {
    const results: AccordionItemProps[] = [];
    if (props.data.length === 0) {
        return [];
    }
    const items = (
        <WarningsErrors title={""} data={props.data.map((md) => md.detail)} />
    );
    results.push({
        id: props.id,
        title: props.title,
        expanded: false,
        content: items,
        headingLevel: "h3",
        className: "",
    });

    return results;
};

export function MessageDetails() {
    const MOCK_MESSAGE_WARNINGS = [
        {
            detail: {
                class: "gov.cdc.prime.router.InvalidCodeMessage",
                fieldMapping: "Specimen_type_code (specimen_type)",
                scope: "item",
                message:
                    "Invalid code: '' is not a display value in altValues set for Specimen_type_code (specimen_type).",
            },
        },
        {
            detail: {
                class: "gov.cdc.prime.router.InvalidEquipmentMessage",
                fieldMapping: "Equipment_Model_ID (equipment_model_id)",
                scope: "item",
                message:
                    "Invalid field Equipment_Model_ID (equipment_model_id); please refer to the Department of Health and Human Services' (HHS) LOINC Mapping spreadsheet for acceptable values.",
            },
        },
    ];
    const navigate = useNavigate();
    const { id } = useParams<MessageDetailsProps>();
    const { messageDetails } = useMessageDetails(id!!);
    // const warnings: WarningError[] = messageDetails
    //     ? messageDetails.warnings
    //     : [];
    const warnings: WarningError[] = MOCK_MESSAGE_WARNINGS;

    const fileUrl = messageDetails?.fileUrl ? messageDetails.fileUrl : "";
    const { fileLocation, sendingOrg, fileName } = parseFileLocation(fileUrl);

    return (
        <>
            <div
                className="grid-container margin-bottom-10"
                data-testid="container"
            >
                <div>
                    <h1>Message ID Search</h1>
                    <Button onClick={() => navigate(-1)} type="button">
                        Back
                    </Button>
                </div>
                <hr className="margin-top-3 margin-bottom-3" />
                <div className="display-flex flex-column">
                    <span className="text-base">Message ID</span>
                    <h2 className="margin-top-1">
                        {messageDetails!.messageId}
                    </h2>
                </div>
                <hr className="margin-top-2 margin-bottom-4" />
                <div className="display-flex flex-column margin-bottom-5">
                    <h2 className="font-sans-lg margin-bottom-4">Sender:</h2>
                    <div className="font-sans-2xs bg-gray-5 radius-md padding-top-3 padding-bottom-4 padding-left-2">
                        <DetailItem
                            item={"Submitter"}
                            content={messageDetails!.sender}
                        ></DetailItem>
                        <DetailItem
                            item={"Incoming Report ID"}
                            content={messageDetails!.reportId}
                        ></DetailItem>
                        <DetailItem
                            item={"Date/Time Submitted"}
                            content={moment(
                                messageDetails!.submittedDate
                            ).format("M/DD/YYYY, h:mm:ss a")}
                        ></DetailItem>
                        <div className="display-flex flex-column margin-bottom-2">
                            <span className="text-base line-height-body-5">
                                {"File Location"}
                            </span>
                            <div>
                                <span className="border-1px bg-primary-lighter radius-md padding-top-4px padding-bottom-4px padding-left-1 padding-right-1">
                                    {fileLocation}
                                </span>
                                <span>{` / ${sendingOrg}`}</span>
                            </div>
                        </div>
                        <DetailItem
                            item={"Incoming File Name"}
                            content={fileName}
                        ></DetailItem>

                        <div className="display-flex flex-column margin-right-2 margin-bottom-1">
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

                        <div className="display-flex flex-column margin-right-2">
                            <Accordion
                                bordered={true}
                                multiselectable={true}
                                items={dataToAccordionItems({
                                    id: "accord-item-errors",
                                    title: `Errors (${
                                        messageDetails!.errors.length
                                    })`,
                                    data: warnings,
                                })}
                            />
                        </div>
                    </div>
                </div>
                <hr className="margin-top-2 margin-bottom-4" />
                <MessageReceivers
                    receiverDetails={messageDetails!.receiverData}
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
