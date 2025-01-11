import { QueryObserverResult } from "@tanstack/react-query";
import { Accordion, Button, Icon, Tag } from "@trussworks/react-uswds";
import type { PropsWithChildren } from "react";
import language from "./language.json";
import type { RSMessage, RSMessageResult } from "../../../config/endpoints/reports";
import Alert, { type AlertProps } from "../../../shared/Alert/Alert";
import { USLinkButton } from "../../USLink";

export interface MessageTestingResultProps extends PropsWithChildren {
    submittedMessage: RSMessage | null;
    resultData: RSMessageResult;
    handleGoBack: () => void;
    refetch: () => Promise<QueryObserverResult<RSMessageResult, Error>>;
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

const MessageTestingResult = ({
    resultData,
    submittedMessage,
    handleGoBack,
    refetch,
    ...props
}: MessageTestingResultProps) => {
    const isPassed =
        !!resultData.senderTransformErrors.length &&
        !!resultData.filterErrors.length &&
        !!resultData.enrichmentSchemaErrors.length &&
        !!resultData.receiverTransformErrors.length;
    const isWarned =
        !!resultData.senderTransformWarnings.length &&
        !!resultData.enrichmentSchemaWarnings.length &&
        !!resultData.receiverTransformWarnings.length;

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

    return (
        <section {...props}>
            <div className="display-flex flex-justify flex-align-center">
                <h2>Test results: {submittedMessage?.fileName}</h2>

                <Button type="button" onClick={() => void refetch()}>
                    Rerun test <Icon.Autorenew className="text-top" />
                </Button>
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

            {errorFields.map((f) => {
                const arr = resultData[f];
                if (arr != null && !Array.isArray(arr)) throw new Error("Invalid resultData");
                if (!arr?.length) return null;

                return (
                    <div key={`${f}-accordion-wrapper`} className="padding-top-4 ">
                        <Accordion
                            key={`${f}-accordion`}
                            items={[
                                {
                                    className: "bg-gray-5",
                                    title: (
                                        <>
                                            <Icon.Error size={3} className="text-top margin-right-1" />
                                            <span className="font-body-lg">Transform warnings</span>
                                            <Tag className="margin-left-1 bg-secondary-vivid">{arr.length}</Tag>
                                        </>
                                    ),
                                    content: (
                                        <div className="bg-white font-sans-sm padding-top-2 padding-bottom-2 padding-left-1 padding-right-1">
                                            {arr.join("\n")}
                                        </div>
                                    ),
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
                                    className: "bg-gray-5",
                                    title: (
                                        <>
                                            <Icon.Warning size={3} className="text-top margin-right-1" />
                                            <span className="font-body-lg">Filters triggered</span>
                                            <Tag className="margin-left-1 bg-accent-warm">{arr.length}</Tag>
                                        </>
                                    ),
                                    content: (
                                        <div className="bg-white font-sans-sm padding-top-2 padding-bottom-2 padding-left-1 padding-right-1">
                                            {arr.join("\n")}
                                        </div>
                                    ),
                                    expanded: false,
                                    headingLevel: "h3",
                                    id: `${f}-list`,
                                },
                            ]}
                        />
                    </div>
                );
            })}
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
                                        {resultData.message}
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
                    key="test-submittedMessage-accordion"
                    items={[
                        {
                            className: "bg-gray-5",
                            title: <span className="font-body-lg">Test message</span>,
                            content: (
                                <div className="bg-white font-sans-sm padding-top-2 padding-bottom-2 padding-left-1 padding-right-1">
                                    {submittedMessage?.reportBody}
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
