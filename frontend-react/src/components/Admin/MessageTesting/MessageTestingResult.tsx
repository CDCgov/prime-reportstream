import { Accordion } from "@trussworks/react-uswds";
import type { PropsWithChildren } from "react";
import language from "./language.json";
import type { RSMessage, RSMessageResult } from "../../../config/endpoints/reports";
import Alert, { type AlertProps } from "../../../shared/Alert/Alert";
import { USLinkButton } from "../../USLink";

export interface MessageTestingResultProps extends PropsWithChildren {
    submittedMessage: RSMessage | null;
    isLoading: boolean;
    resultData: any; // temporary typing
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

const MessageTestingResult = ({ resultData, submittedMessage, ...props }: MessageTestingResultProps) => {
    const isPassed =
        resultData.senderTransformPassed &&
        resultData.filtersPassed &&
        resultData.enrichmentSchemaPassed &&
        resultData.receiverTransformPassed;
    const isWarned =
        !!resultData.senderTransformWarnings.length &&
        !!resultData.enrichmentSchemaWarnings.length &&
        !!resultData.receiverTransformWarnings;

    const alertType: AlertProps["type"] = !isPassed ? "error" : isWarned ? "warning" : "success";
    const alertHeading = language[`${alertType}AlertHeading`];
    const alertBody = language[`${alertType}AlertBody`];

    return (
        <section {...props}>
            <h2>Test results: {submittedMessage?.fileName}</h2>
            <USLinkButton>{"<"} Select new submittedMessage</USLinkButton>
            Test run: {submittedMessage?.dateCreated}
            <Alert type={alertType} heading={alertHeading}>
                {alertBody}
            </Alert>
            {errorFields.map((f) => {
                const arr = resultData[f];
                if (arr != null && !Array.isArray(arr)) throw new Error("Invalid resultData");
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
                const arr = resultData[f];
                if (arr != null && !Array.isArray(arr)) throw new Error("Invalid resultData");
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
            {resultData.message && (
                <Accordion
                    key={`output-submittedMessage-accordion`}
                    items={[
                        {
                            title: "Output submittedMessage",
                            content: resultData.message,
                            expanded: false,
                            headingLevel: "h3",
                            id: `output-submittedMessage-list`,
                        },
                    ]}
                />
            )}
            <Accordion
                key="test-submittedMessage-accordion"
                items={[
                    {
                        title: "Test submittedMessage",
                        content: submittedMessage?.reportBody,
                        expanded: false,
                        headingLevel: "h3",
                        id: "test-submittedMessage-list",
                    },
                ]}
            />
        </section>
    );
};

export default MessageTestingResult;
