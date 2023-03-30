import { GridContainer } from "@trussworks/react-uswds";

import { IACardGridProps } from "../../components/Content/Templates/IACardGridTemplate";
import {
    directoryArrayToMap,
    RESOURCE_INDEX_SECTIONS,
    resourceIndexSections,
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
        directories: directoryArrayToMap(
            resourcesDirectories,
            resourceIndexSections
        ),
        sectionOrder: [
            RESOURCE_INDEX_SECTIONS.DEFAULT,
            RESOURCE_INDEX_SECTIONS.TESTING_FACILITIES,
            RESOURCE_INDEX_SECTIONS.PUBLIC_HEALTH_DEPARTMENTS,
        ],
    },
    includeRouter: true,
};
/** Use this component in the main App Router! It will handle rendering everything
 * and set the Helmet values */
export function Resources() {
    return (
        <GridContainer>
            <IATemplate {...rootProps} />
        </GridContainer>
    );
}
