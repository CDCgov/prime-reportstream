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
    directories: resourcesDirectories,
    pageName: ResourcesDirectoryTools.title,
    templateKey: TemplateName.CARD_GRID,
    templateProps: {
        title: ResourcesDirectoryTools.title,
        subtitle: ResourcesDirectoryTools.subtitle,
        directoriesToRender: resourcesDirectories,
    },
};
/** Use this component in the main App Router! It will handle rendering everything
 * and set the Helmet values */
export const Resources = () => <IATemplate {...rootProps} {...rootProps} />;
