import { QueryObserverResult } from "@tanstack/react-query";
import { Accordion, Button, Icon } from "@trussworks/react-uswds";
import { type PropsWithChildren, useState } from "react";
import language from "./language.json";
import { MessageTestingAccordion } from "./MessageTestingAccordion";
import type { RSMessage, RSMessageResult } from "../../../config/endpoints/reports";
import Alert, { type AlertProps } from "../../../shared/Alert/Alert";
import { USLink, USLinkButton } from "../../USLink";
import ReactPDF, { Page, Text, View, Document, StyleSheet, pdf, PDFDownloadLink, usePDF } from "@react-pdf/renderer";

export interface MessageTestingResultProps extends PropsWithChildren {
    submittedMessage: RSMessage | null;
    resultData: RSMessageResult;
    handleGoBack: () => void;
    refetch: () => Promise<QueryObserverResult<RSMessageResult, Error>>;
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
    ...props
}: MessageTestingResultProps) => {
    const isPassed =
        resultData.senderTransformErrors.length === 0 &&
        resultData.filterErrors.length === 0 &&
        resultData.enrichmentSchemaErrors.length === 0 &&
        resultData.receiverTransformErrors.length === 0;
    const isWarned =
        resultData.senderTransformWarnings.length > 0 &&
        resultData.enrichmentSchemaWarnings.length > 0 &&
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

    const styles = StyleSheet.create({
        page: {
            flexDirection: "row",
            backgroundColor: "#E4E4E4",
        },
        section: {
            margin: 10,
            padding: 10,
            flexGrow: 1,
        },
    });

    const MyDocument = (
        <Document>
            <Page size="A4" style={styles.page}>
                <View style={styles.section}>
                    <Text>Section #1</Text>
                </View>
                <View style={styles.section}>
                    <Text>{resultData.message}</Text>
                </View>
            </Page>
        </Document>
    );
    const [instance, updateInstance] = usePDF({ document: MyDocument });
    return (
        <section {...props}>
            <div className="display-flex flex-justify flex-align-center">
                <h2>Test results: {submittedMessage?.fileName}</h2>

                <USLinkButton href={instance.url} download="test.pdf" type="button" outline>
                    {instance.loading ? "Loading..." : "Download PDF"} <Icon.ArrowDropDown className="text-top" />
                </USLinkButton>

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
                    key={`test-submittedMessage-accordion`}
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
