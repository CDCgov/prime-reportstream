import { MarkdownContent } from "../../components/Content/MarkdownContent";
import ReferralGuide from "../../content/resources/referral-guide.md";

/** Markdown adapters. These are what are required to render MD as a page */
export const ReferralGuideMd = () => (
    <MarkdownContent markdownUrl={ReferralGuide} />
);
