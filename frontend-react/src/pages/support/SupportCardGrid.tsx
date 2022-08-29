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
    directories: supportDirectories,
    pageName: SupportDirectoryTools.title,
    templateKey: TemplateName.CARD_GRID,
    templateProps: {
        title: SupportDirectoryTools.title,
        subtitle: SupportDirectoryTools.subtitle,
        directoriesToRender: supportDirectories,
    },
};
/** Use this component in the main App Router! It will handle rendering everything
 * and set the Helmet values */
export const Support = () => <IATemplate {...templateProps} />;
