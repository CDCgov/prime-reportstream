import { MarkdownRenderer } from "../../components/Content/MarkdownRenderer";
import ReferralGuide from "../../content/resources/referral-guide.md";

/** Markdown adapters. These are what are required to render MD as a page */
export const ReferralGuideMd = () => (
    <MarkdownRenderer markdownUrl={ReferralGuide} />
);
