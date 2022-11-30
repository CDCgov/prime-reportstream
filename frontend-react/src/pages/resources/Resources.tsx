import { IACardGridProps } from "../../components/Content/Templates/IACardGridTemplate";
import {
    resourcesDirectories,
    ResourcesDirectoryTools,
} from "../../content/resources";
import {
    IATemplate,
    IATemplateProps,
    TemplateName,
} from "../../components/Content/Templates/IATemplate";

const rootProps: IATemplateProps<IACardGridProps> = {
    pageName: ResourcesDirectoryTools.title,
    subtitle: ResourcesDirectoryTools.subtitle,
    templateKey: TemplateName.CARD_GRID,
    templateProps: {
        pageName: ResourcesDirectoryTools.title,
        subtitle: ResourcesDirectoryTools.subtitle,
        directories: resourcesDirectories,
    },
    includeRouter: true,
};
/** Use this component in the main App Router! It will handle rendering everything
 * and set the Helmet values */
export const Resources = () => <IATemplate {...rootProps} {...rootProps} />;
