import { Accordion } from "@trussworks/react-uswds";
import type { PropsWithChildren } from "react";
import language from "./language.json";
import type { RSSubmittedMessage } from "./MessageTestingForm";
import type { RSMessageResult } from "../../../config/endpoints/reports";
import Alert, { type AlertProps } from "../../../shared/Alert/Alert";
import { USLinkButton } from "../../USLink";

export interface MessageTestingResultProps extends PropsWithChildren {
    message: RSSubmittedMessage;
    result: RSMessageResult;
}

const errorFields: (keyof RSMessageResult)[] = [
    "senderTransformErrors",
    "enrichmentSchemaErrors",
    "receiverTransformErrors",
    "filterErrors",
];
const warningFields: (keyof RSMessageResult)[] = [
    "senderTransformWarnings",
    "enrichmentSchemaWarnings",
    "receiverTransformWarnings",
];

const MessageTestingResult = ({ result, message, ...props }: MessageTestingResultProps) => {
    const isPassed =
        result.senderTransformPassed &&
        result.filtersPassed &&
        result.enrichmentSchemaPassed &&
        result.receiverTransformPassed;
    const isWarned =
        !!result.senderTransformWarnings.length &&
        !!result.enrichmentSchemaWarnings.length &&
        !!result.receiverTransformWarnings;

    const alertType: AlertProps["type"] = !isPassed ? "error" : isWarned ? "warning" : "success";
    const alertHeading = language[`${alertType}AlertHeading`];
    const alertBody = language[`${alertType}AlertBody`];

    return (
        <section {...props}>
            <h2>Test results: {message.fileName}</h2>
            <USLinkButton>{"<"} Select new message</USLinkButton>
            Test run: {message.dateCreated.toISOString()}
            <Alert type={alertType} heading={alertHeading}>
                {alertBody}
            </Alert>
            {errorFields.map((f) => {
                const arr = result[f];
                if (arr != null && !Array.isArray(arr)) throw new Error("Invalid result");
                if (!arr?.length) return null;

                return (
                    <Accordion
                        key={`${f}-accordion`}
                        items={[
                            { title: f, content: arr.join("\n"), expanded: false, headingLevel: "h3", id: `${f}-list` },
                        ]}
                    />
                );
            })}
            {warningFields.map((f) => {
                const arr = result[f];
                if (arr != null && !Array.isArray(arr)) throw new Error("Invalid result");
                if (!arr?.length) return null;

                return (
                    <Accordion
                        key={`${f}-accordion`}
                        items={[
                            { title: f, content: arr.join("\n"), expanded: false, headingLevel: "h3", id: `${f}-list` },
                        ]}
                    />
                );
            })}
            {result.message && (
                <Accordion
                    key={`output-message-accordion`}
                    items={[
                        {
                            title: "Output message",
                            content: result.message,
                            expanded: false,
                            headingLevel: "h3",
                            id: `output-message-list`,
                        },
                    ]}
                />
            )}
            <Accordion
                key="test-message-accordion"
                items={[
                    {
                        title: "Test message",
                        content: message.reportBody,
                        expanded: false,
                        headingLevel: "h3",
                        id: "test-message-list",
                    },
                ]}
            />
        </section>
    );
};

export default MessageTestingResult;
