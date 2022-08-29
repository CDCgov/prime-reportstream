import { MarkdownDirectory } from "../../components/Content/MarkdownDirectory";
import markdownPagesGuide from "../../content/internal-user-guides/make-markdown-pages.md";
import IASideNavTemplate from "../../components/Content/Templates/IASideNavTemplate";
import { AuthElement } from "../../components/AuthElement";
import { MemberType } from "../../hooks/UseOktaMemberships";

export const InternalUserGuidesDirectory = [
    new MarkdownDirectory()
        .setTitle("Create markdown pages")
        .setSlug("create-markdown-pages")
        .addFile(markdownPagesGuide),
];

const InternalUserGuides = () => {
    return <IASideNavTemplate directories={InternalUserGuidesDirectory} />;
};

export default InternalUserGuides;

export const InternalUserGuidesWithAuth = () => (
    <AuthElement
        element={InternalUserGuides}
        requiredUserType={MemberType.PRIME_ADMIN}
    />
);
