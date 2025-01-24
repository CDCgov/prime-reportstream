import { Document, Font, Page, StyleSheet, Text, usePDF, View } from "@react-pdf/renderer";
import { QueryObserverResult } from "@tanstack/react-query";
import { Accordion, Button, Icon } from "@trussworks/react-uswds";
import PublicSansRegular from "@uswds/uswds/fonts/public-sans/PublicSans-Regular.ttf";
import PublicSansBold from "@uswds/uswds/fonts/public-sans/PublicSans-Bold.ttf";
import { type PropsWithChildren } from "react";
import language from "./language.json";
import { MessageTestingAccordion } from "./MessageTestingAccordion";
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

    const exampleData = {
        orgName: "Example Organization",
        receiverName: "John Doe",
        testStatus: "failed", // can be 'failed' or 'warning'
        filtersTriggered: ["OBR|1|...", "OBX|1|...", "OBX|2|..."],
        outputMessage: `MSH|^~\\&|SOMEHL7MESSAGE
    PID|1|...
    OBR|1|...`,
        testMessage: `{
        "resourceType": "Patient",
        "id": "12345",
        "name": [
          {
            "use": "official",
            "family": "Doe",
            "given": ["John", "A."]
          }
        ]
      }`,
    };

    Font.register({
        family: "Public Sans Web",
        fonts: [{ src: PublicSansRegular }, { src: PublicSansBold, fontWeight: "bold" }],
    });

    const styles = StyleSheet.create({
        page: {
            padding: 30,
            fontSize: 12,
            fontFamily: "Public Sans Web",
        },
        section: {
            marginBottom: 20,
        },
        sectionTitle: {
            fontSize: 14,
            marginBottom: 6,
            fontWeight: "bold",
        },
        text: {
            marginBottom: 4,
        },
        bannerContainer: {
            padding: 10,
            marginBottom: 20,
        },
        bannerText: {
            color: "black",
            fontWeight: "bold",
        },
        bannerWarning: {
            backgroundColor: "#faf3d1",
            borderLeft: "4px solid #ffbe2e",
        },
        bannerError: {
            backgroundColor: "#f4e3db",
            borderLeft: "4px solid #d54309",
        },
        codeBlock: {
            backgroundColor: "#f5f5f5",
            padding: 10,
            fontFamily: "Courier", // or 'Roboto Mono' if you registered it
            marginBottom: 10,
        },
        hr: {
            borderBottomWidth: 1,
            borderBottomColor: "#000",
            marginVertical: 5,
        },
    });

    const PDFDocument = ({
        orgName,
        receiverName,
        testStatus, // 'failed' or 'warning'
        filtersTriggered, // array of strings
        outputMessage, // HL7 text
        testMessage, // JSON text
    }) => {
        const bannerText = testStatus === "failed" ? "Test failed" : "Warnings";
        const lines = prettifyJSON(testMessage)
            .split("\n")
            .map((line) => {
                return line.replace(/ /g, "\u00A0");
            });
        return (
            <Document>
                <Page style={styles.page}>
                    {/* Section 1 */}
                    <View style={styles.section}>
                        <Text style={styles.sectionTitle}>Message testing result</Text>
                        <Text style={styles.text}>Org name: {orgName}</Text>
                        <Text style={styles.text}>Receiver name: {receiverName}</Text>
                    </View>

                    {/* Section 2 - Banner */}
                    <View
                        style={[
                            styles.bannerContainer,
                            { ...(testStatus === "failed" ? styles.bannerError : styles.bannerWarning) },
                        ]}
                    >
                        <Text style={styles.bannerText}>{bannerText}</Text>
                    </View>

                    {/* Section 3 - Filters triggered */}
                    <View style={styles.section}>
                        <Text style={styles.sectionTitle}>Filters triggered</Text>
                        {filtersTriggered?.map((line, index) => (
                            <View key={`filter-line-${index}`}>
                                <View style={styles.codeBlock}>
                                    <Text>{line}</Text>
                                </View>
                            </View>
                        ))}
                    </View>

                    {/* Section 4 - Output message (HL7) */}
                    <View style={styles.section}>
                        <Text style={styles.sectionTitle}>Output message</Text>
                        <View style={styles.codeBlock}>
                            <Text>{outputMessage}</Text>
                        </View>
                    </View>

                    {/* Section 5 - Test message (JSON) */}
                    <View style={styles.section}>
                        <Text style={styles.sectionTitle}>Test message</Text>
                        <View style={styles.codeBlock}>
                            {lines.map((line, idx) => (
                                <Text key={idx}>{line}</Text>
                            ))}
                        </View>
                    </View>
                </Page>
            </Document>
        );
    };

    const MyDocument = <PDFDocument {...exampleData} />;
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
