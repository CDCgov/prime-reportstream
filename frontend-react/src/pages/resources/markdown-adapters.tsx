import { MarkdownRenderer } from "../../components/Content/MarkdownRenderer";
import ReferralGuide from "../../content/resources/referral-guide.md?url";
import GettingStartedSubmittingData from "../../content/resources/getting-started-submitting-data.md?url";

/** Markdown adapters. These are what are required to render MD as a page */
export const ReferralGuideMd = () => (
    <MarkdownRenderer markdownUrl={ReferralGuide} />
);

export const GettingStartedSubmittingDataMd = () => (
    <MarkdownRenderer markdownUrl={GettingStartedSubmittingData} />
);
