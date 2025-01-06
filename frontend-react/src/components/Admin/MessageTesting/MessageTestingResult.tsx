import { Accordion, Icon, Tag } from "@trussworks/react-uswds";
import type { PropsWithChildren } from "react";
import language from "./language.json";
import type { RSMessage, RSMessageResult } from "../../../config/endpoints/reports";
import Alert, { type AlertProps } from "../../../shared/Alert/Alert";
import { USLinkButton } from "../../USLink";
import { camelCaseToTitle } from "../../../utils/misc";

export interface MessageTestingResultProps extends PropsWithChildren {
    submittedMessage: RSMessage | null;
    resultData: any; // temporary typing
    handleGoBack: () => void;
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

const MessageTestingResult = ({ resultData, submittedMessage, handleGoBack, ...props }: MessageTestingResultProps) => {
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
    const timeOptions = {
        timeZone: "UTC",
        month: "2-digit",
        day: "2-digit",
        year: "numeric",
        hour: "numeric",
        minute: "numeric",
        hour12: true,
    };
    const dateCreated = new Date(submittedMessage?.dateCreated);
    console.log("resultData = ", resultData);
    return (
        <section {...props}>
            <h2>Test results: {submittedMessage?.fileName}</h2>
            <USLinkButton onClick={handleGoBack} className="text-no-underline" unstyled>
                {"<"} Select new message
            </USLinkButton>

            <div>
                <p>Test run: {dateCreated.toLocaleString("en-US", timeOptions)} UTC</p>
            </div>

            <div>
                <Alert type={alertType} heading={alertHeading}>
                    {alertBody}
                </Alert>
            </div>

            {errorFields.map((f) => {
                const arr = resultData[f];
                if (arr != null && !Array.isArray(arr)) throw new Error("Invalid resultData");
                if (!arr?.length) return null;

                return (
                    <div key={`${f}-accordion-wrapper`} className="padding-top-4">
                        <Accordion
                            key={`${f}-accordion`}
                            items={[
                                {
                                    title: (
                                        <>
                                            <Icon.Error className="text-ttop margin-right-05" />
                                            {camelCaseToTitle(f)}
                                            <Tag className="margin-left-05 bg-secondary-vivid">{arr.length}</Tag>
                                        </>
                                    ),
                                    content: arr.join("\n"),
                                    expanded: false,
                                    headingLevel: "h3",
                                    id: `${f}-list`,
                                },
                            ]}
                        />
                    </div>
                );
            })}

            {warningFields.map((f) => {
                const arr = resultData[f];
                if (arr != null && !Array.isArray(arr)) throw new Error("Invalid resultData");
                if (!arr?.length) return null;

                return (
                    <div key={`${f}-accordion-wrapper`} className="padding-top-4">
                        <Accordion
                            key={`${f}-accordion`}
                            items={[
                                {
                                    title: (
                                        <>
                                            <Icon.Warning className="text-ttop margin-right-05" />
                                            {camelCaseToTitle(f)}
                                            <Tag className="margin-left-05 bg-accent-warm">{arr.length}</Tag>
                                        </>
                                    ),
                                    content: arr.join("\n"),
                                    expanded: false,
                                    headingLevel: "h3",
                                    id: `${f}-list`,
                                },
                            ]}
                        />
                    </div>
                );
            })}
            {resultData.message && (
                <div key={`output-submittedMessage-accordion-wrapper`} className="padding-top-4">
                    <Accordion
                        key={`output-submittedMessage-accordion`}
                        items={[
                            {
                                title: "Output message",
                                content: resultData.message,
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
                    key="test-submittedMessage-accordion"
                    items={[
                        {
                            title: "Test message",
                            content: submittedMessage?.reportBody,
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
