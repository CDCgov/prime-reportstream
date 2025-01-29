import { usePDF } from "@react-pdf/renderer";
import { QueryObserverResult } from "@tanstack/react-query";
import { Accordion, Button, Icon } from "@trussworks/react-uswds";
import { type PropsWithChildren } from "react";
import language from "./language.json";
import { MessageTestingAccordion } from "./MessageTestingAccordion";
import MessageTestingPDF from "./MessageTestingPDF";
import type { RSMessage, RSMessageResult } from "../../../config/endpoints/reports";
import Alert, { type AlertProps } from "../../../shared/Alert/Alert";
import HL7Message from "../../../shared/HL7Message/HL7Message";
import { prettifyJSON } from "../../../utils/misc";
import { USLinkButton } from "../../USLink";

export interface MessageTestingResultProps extends PropsWithChildren {
    submittedMessage: RSMessage | null;
    resultData: RSMessageResult;
    handleGoBack: () => void;
    refetch: () => Promise<QueryObserverResult<RSMessageResult, Error>>;
    orgname: string;
    receivername: string;
}

const filterFields: (keyof RSMessageResult)[] = ["filterErrors"];

const transformFields: (keyof RSMessageResult)[] = [
    "senderTransformErrors",
    "enrichmentSchemaErrors",
    "receiverTransformErrors",
];

const warningFields: (keyof RSMessageResult)[] = [
    "senderTransformWarnings",
    "enrichmentSchemaWarnings",
    "receiverTransformWarnings",
];

const MessageTestingResult = ({
    resultData,
    submittedMessage,
    handleGoBack,
    refetch,
    orgname,
    receivername,
    ...props
}: MessageTestingResultProps) => {
    const isPassed =
        resultData.senderTransformErrors.length === 0 &&
        resultData.filterErrors.length === 0 &&
        resultData.enrichmentSchemaErrors.length === 0 &&
        resultData.receiverTransformErrors.length === 0;
    const isWarned =
        resultData.senderTransformWarnings.length > 0 ||
        resultData.enrichmentSchemaWarnings.length > 0 ||
        resultData.receiverTransformWarnings.length > 0;

    const alertType: AlertProps["type"] = !isPassed ? "error" : isWarned ? "warning" : "success";
    const alertHeading = language[`${alertType}AlertHeading`];
    const alertBody = language[`${alertType}AlertBody`];
    const timeOptions: Intl.DateTimeFormatOptions = {
        timeZone: "UTC",
        month: "2-digit",
        day: "2-digit",
        year: "numeric",
        hour: "numeric",
        minute: "numeric",
        hour12: true,
    };

    const MessageTestingPDFRef = (
        <MessageTestingPDF
            orgName={orgname}
            receiverName={receivername}
            testStatus={alertType}
            filtersTriggered={resultData.filterErrors}
            testMessage={submittedMessage?.reportBody ?? ""}
        />
    );
    const [instance] = usePDF({ document: MessageTestingPDFRef });
    return (
        <section {...props}>
            <div className="display-flex flex-justify flex-align-center">
                <h2>Test results: {submittedMessage?.fileName}</h2>
                <div>
                    <USLinkButton
                        href={instance.url ?? ""}
                        download={`message-testing-result_${Date.now()}.pdf`}
                        type="button"
                        outline
                    >
                        {instance.loading ? "Loading..." : "Download PDF"} <Icon.ArrowDropDown className="text-top" />
                    </USLinkButton>

                    <Button className="margin-left-1" type="button" onClick={() => void refetch()}>
                        Rerun test <Icon.Autorenew className="text-top" />
                    </Button>
                </div>
            </div>
            <USLinkButton onClick={handleGoBack} className="text-no-underline text-bold" unstyled>
                <Icon.NavigateBefore className="text-top" /> Select new message
            </USLinkButton>

            <div>
                <p>
                    Test run:{" "}
                    {submittedMessage?.dateCreated
                        ? `${new Date(submittedMessage.dateCreated).toLocaleString("en-US", timeOptions)} UTC`
                        : "N/A"}{" "}
                </p>
            </div>

            <div>
                <Alert type={alertType} heading={alertHeading}>
                    {alertBody}
                </Alert>
            </div>

            <MessageTestingAccordion
                accordionTitle="Filters triggered"
                priority="error"
                resultData={resultData}
                fieldsToRender={filterFields}
            />

            <MessageTestingAccordion
                accordionTitle="Transform errors"
                priority="error"
                resultData={resultData}
                fieldsToRender={transformFields}
            />

            <MessageTestingAccordion
                accordionTitle="Transform warnings"
                priority="warning"
                resultData={resultData}
                fieldsToRender={warningFields}
            />

            {resultData.message && isPassed && (
                <div key={`output-submittedMessage-accordion-wrapper`} className="padding-top-4">
                    <Accordion
                        key={`output-submittedMessage-accordion`}
                        items={[
                            {
                                className: "bg-gray-5",
                                title: <span className="font-body-lg">Output message</span>,
                                content: (
                                    <div className="bg-white font-sans-sm padding-top-2 padding-bottom-2 padding-left-1 padding-right-1">
                                        <HL7Message message={resultData.message} />
                                    </div>
                                ),
                                expanded: false,
                                headingLevel: "h3",
                                id: `output-submittedMessage-list`,
                            },
                        ]}
                    />
                </div>
            )}
            <div key={`test-submittedMessage-accordion-wrapper`} className="padding-top-4">
                <Accordion
                    key={`test-submittedMessage-accordion`}
                    items={[
                        {
                            className: "bg-gray-5",
                            title: <span className="font-body-lg">Test message</span>,
                            content: (
                                <div className="bg-white font-sans-sm padding-top-2 padding-bottom-2 padding-left-1 padding-right-1">
                                    <pre>{prettifyJSON(submittedMessage?.reportBody ?? "")}</pre>
                                </div>
                            ),
                            expanded: false,
                            headingLevel: "h3",
                            id: "test-submittedMessage-list",
                        },
                    ]}
                />
            </div>
        </section>
    );
};

export default MessageTestingResult;
