import { Document, Font, Page, StyleSheet, Text, View } from "@react-pdf/renderer";
import PublicSansBold from "@uswds/uswds/fonts/public-sans/PublicSans-Bold.ttf";
import PublicSansRegular from "@uswds/uswds/fonts/public-sans/PublicSans-Regular.ttf";
import language from "./language.json";
import { RSFilterError } from "../../../config/endpoints/reports";
import { prettifyJSON } from "../../../utils/misc";

interface MessageTestingPDFProps {
    orgName: string;
    receiverName: string;
    testStatus: string;
    filterFieldData: RSFilterError[];
    transformFieldData: string[];
    warningFieldData: string[];
    testMessage: string;
    outputMessage: string;
    isPassed: boolean;
}

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
    bannerSuccess: {
        backgroundColor: "#ecf3ec",
        borderLeft: "4px solid #00a91c",
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

const statusConfig = {
    success: {
        text: language.successAlertHeading,
        className: styles.bannerSuccess,
    },
    warning: {
        text: language.warningAlertHeading,
        className: styles.bannerWarning,
    },
    error: {
        text: language.errorAlertHeading,
        className: styles.bannerError,
    },
};

const MessageTestingPDF = ({
    orgName,
    receiverName,
    testStatus,
    filterFieldData,
    transformFieldData,
    warningFieldData,
    testMessage,
    outputMessage,
    isPassed,
}: MessageTestingPDFProps) => {
    const { text: bannerText, className: bannerClass } = statusConfig[testStatus as "success" | "warning" | "error"];
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
                <View style={[bannerClass, styles.bannerContainer]}>
                    <Text style={styles.bannerText}>{bannerText}</Text>
                </View>

                {/* Section 3 - Filters triggered */}
                {filterFieldData.length && (
                    <View style={styles.section}>
                        <Text style={styles.sectionTitle}>Filters triggered</Text>
                        {filterFieldData.map((data, index) => (
                            <View key={`filter-line-${index}`}>
                                <View style={styles.codeBlock}>
                                    <Text>
                                        {data.filter}: {data.message}
                                    </Text>
                                </View>
                            </View>
                        ))}
                    </View>
                )}

                {/* Section 4 - Transforms triggered */}
                {transformFieldData.length && (
                    <View style={styles.section}>
                        <Text style={styles.sectionTitle}>Transform errors</Text>
                        {transformFieldData.map((line, index) => (
                            <View key={`filter-line-${index}`}>
                                <View style={styles.codeBlock}>
                                    <Text>{line}</Text>
                                </View>
                            </View>
                        ))}
                    </View>
                )}

                {/* Section 5 - Warnings triggered */}
                {warningFieldData.length && (
                    <View style={styles.section}>
                        <Text style={styles.sectionTitle}>Transform warnings</Text>
                        {warningFieldData.map((line, index) => (
                            <View key={`filter-line-${index}`}>
                                <View style={styles.codeBlock}>
                                    <Text>{line}</Text>
                                </View>
                            </View>
                        ))}
                    </View>
                )}

                {/* Section 6 - Output message (HL7) */}
                {isPassed && (
                    <View style={styles.section}>
                        <Text style={styles.sectionTitle}>Output message</Text>
                        <View style={styles.codeBlock}>
                            <Text>{outputMessage}</Text>
                        </View>
                    </View>
                )}

                {/* Section 7 - Test message (JSON) */}
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

export default MessageTestingPDF;
