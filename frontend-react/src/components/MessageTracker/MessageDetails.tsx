import React from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@trussworks/react-uswds";

import { AuthElement } from "../AuthElement";
import { withCatchAndSuspense } from "../RSErrorBoundary";
import { MemberType } from "../../hooks/UseOktaMemberships";
import { ErrorPage } from "../../pages/error/ErrorPage";

import { WarningsErrors } from "./WarningsErrors";
import { MessageSender } from "./MessageSender";
import { QualityFilters } from "./QualityFilters";

// type MessageDetailsProps = {
//     messageId: string | undefined;
// };

function MessageDetailsSender() {
    const MOCK_MESSAGE_SENDER_DETAILS = {
        id: 1,
        messageId: "12-234567",
        sender: "somebody 1",
        submittedDate: "09/28/2022",
        reportId: "29038fca-e521-4af8-82ac-6b9fafd0fd58",
        fileUrl: "https://someurl",
    };

    // const { messageId } = useParams<MessageDetailsProps>();
    // TODO: call endpoint with messageId to get details
    const senderDetails = MOCK_MESSAGE_SENDER_DETAILS;

    if (!senderDetails) return <ErrorPage type="page" />;

    return <MessageSender senderDetails={senderDetails} />;
}

function Warnings() {
    const MOCK_MESSAGE_WARNINGS = [
        {
            field: "first field",
            description:
                "Invalid code: '' is not a display value in altValues set for Specimen_type_code (specimen_type).",
            type: "Invalid code",
            trackingIds: ["first_id1", "first_id3", "first_id4"],
        },
        {
            field: "second field",
            description:
                "Missing Ordering_facility_email (ordering_facility_email) header",
            type: "Missing email",
            trackingIds: ["second_id"],
        },
    ];

    // const { messageId } = useParams<MessageDetailsProps>();
    // TODO: call endpoint with messageId to get warnings
    const warnings = MOCK_MESSAGE_WARNINGS;

    return <WarningsErrors title={"Warnings:"} data={warnings} />;
}

function Errors() {
    const MOCK_MESSAGE_ERRORS = [
        {
            field: "first field",
            description: "Missing required HL7 message type",
            type: "Missing type",
            trackingIds: ["first_id1", "first_id3", "first_id4"],
        },
        {
            field: "second field",
            description: "Expecting content type of 'application/hl7 -v2",
            type: "Invalid content type",
            trackingIds: ["second_id"],
        },
    ];

    // const { messageId } = useParams<MessageDetailsProps>();
    // TODO: call endpoint with messageId to get errors
    const errors = MOCK_MESSAGE_ERRORS;

    return <WarningsErrors title={"Errors:"} data={errors} />;
}

function Jurisdictions() {
    const MOCK_MESSAGE_QUALITY_FILTERS = [
        {
            organization: "Alaska Public Health Department",
            organization_id: "ak-phd",
            service: "elr",
            itemCount: 2,
            itemCountBeforeQualityFiltering: 5,
            filteredReportRows: [
                "Filtered out item Alaska1",
                "Filtered out item Alaska2",
                "Filtered out item Alaska4",
            ],
            filteredReportItems: [
                {
                    filterType: "QUALITY_FILTER",
                    filterName: "hasValidDataFor",
                    filteredTrackingElement: "Alaska1",
                    filterArgs: ["patient_dob"],
                    message: "Filtered out item Alaska1",
                },
            ],
            sentReports: [],
            sending_at: "",
        },
        {
            organization: "Hawaii Public Health Department",
            organization_id: "hi-phd",
            service: "elr",
            itemCount: 2,
            itemCountBeforeQualityFiltering: 5,
            filteredReportRows: [
                "Filtered out item Hawaii6",
                "Filtered out item Hawaii7",
                "Filtered out item Hawaii9",
            ],
            filteredReportItems: [
                {
                    filterType: "QUALITY_FILTER",
                    filterName: "hasValidDataFor",
                    filteredTrackingElement: "Hawaii6",
                    filterArgs: ["specimen_type"],
                    message: "Filtered out item Hawaii6",
                },
                {
                    filterType: "QUALITY_FILTER",
                    filterName: "hasValidDataFor",
                    filteredTrackingElement: "Hawaii7",
                    filterArgs: ["specimen_type"],
                    message: "Filtered out item Hawaii7",
                },
                {
                    filterType: "QUALITY_FILTER",
                    filterName: "hasValidDataFor",
                    filteredTrackingElement: "Hawaii9",
                    filterArgs: ["specimen_type"],
                    message: "Filtered out item Hawaii9",
                },
            ],
            sentReports: [],
            sending_at: "",
        },
    ];

    // const { messageId } = useParams<MessageDetailsProps>();
    // TODO: call endpoint with messageId to get errors
    const qualityFilters = MOCK_MESSAGE_QUALITY_FILTERS;

    return <QualityFilters qualityFilters={qualityFilters} />;
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
                {withCatchAndSuspense(<MessageDetailsSender />)}
                {withCatchAndSuspense(<Warnings />)}
                {withCatchAndSuspense(<Errors />)}
                {withCatchAndSuspense(<Jurisdictions />)}
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
