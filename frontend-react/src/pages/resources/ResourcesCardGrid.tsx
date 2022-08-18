import {
    IACardGridProps,
    IACardGridTemplate,
} from "../../components/Content/Templates/IACardGridTemplate";
import {
    IAMetaAndRouter,
    IAMetaAndRouterProps,
} from "../../components/Content/Templates/IAMetaAndRouter";
import {
    resourcesContentMap,
    resourcesDirectories,
    ResourcesDirectoryTools,
} from "../../content/resources";

const pageProps: IACardGridProps = {
    title: ResourcesDirectoryTools.title,
    subtitle: ResourcesDirectoryTools.subtitle,
    directoriesToRender: resourcesContentMap,
};
/** This is our main page content */
export const ResourcesCardGrid = () => <IACardGridTemplate {...pageProps} />;

const rootProps: IAMetaAndRouterProps = {
    path: ResourcesDirectoryTools.root,
    pageName: ResourcesDirectoryTools.title,
    indexComponent: ResourcesCardGrid,
    directoriesToRoute: resourcesDirectories,
};
/** Use this component in the main App Router! It will handle rendering everything
 * and set the Helmet values */
export const Resources = () => <IAMetaAndRouter {...rootProps} />;
