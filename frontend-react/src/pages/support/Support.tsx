import { IACardGridProps } from "../../components/Content/Templates/IACardGridTemplate";
import {
    supportDirectories,
    SupportDirectoryTools,
} from "../../content/support";
import {
    IATemplate,
    IATemplateProps,
    TemplateName,
} from "../../components/Content/Templates/IATemplate";

const templateProps: IATemplateProps<IACardGridProps> = {
    pageName: SupportDirectoryTools.title,
    subtitle: SupportDirectoryTools.subtitle,
    templateKey: TemplateName.CARD_GRID,
    templateProps: {
        pageName: SupportDirectoryTools.title,
        subtitle: SupportDirectoryTools.subtitle,
        directories: supportDirectories,
    },
    includeRouter: true,
};
/** Use this component in the main App Router! It will handle rendering everything
 * and set the Helmet values */
export function Support() {
    return (
        <article id="main-content" className="usa-prose tablet:grid-col-12">
            <IATemplate {...templateProps} />
        </article>
    );
}
