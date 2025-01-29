import { Document, Font, Page, StyleSheet, Text, View } from "@react-pdf/renderer";
import PublicSansBold from "@uswds/uswds/fonts/public-sans/PublicSans-Bold.ttf";
import PublicSansRegular from "@uswds/uswds/fonts/public-sans/PublicSans-Regular.ttf";
import { prettifyJSON } from "../../../utils/misc";

interface MessageTestingPDFProps {
    orgName: string;
    receiverName: string;
    testStatus: string;
    filtersTriggered: string[];
    testMessage: string;
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

const MessageTestingPDF = ({
    orgName,
    receiverName,
    testStatus,
    filtersTriggered,
    testMessage,
}: MessageTestingPDFProps) => {
    const bannerText = testStatus === "error" ? "Test failed" : "Test passed with warnings";
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
                        { ...(testStatus === "error" ? styles.bannerError : styles.bannerWarning) },
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

                {/* Section 4 - Test message (JSON) */}
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
