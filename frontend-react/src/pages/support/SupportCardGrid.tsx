import {
    IACardGridProps,
    IACardGridTemplate,
} from "../../components/Content/Templates/IACardGridTemplate";
import {
    IAMetaAndRouter,
    IAMetaAndRouterProps,
} from "../../components/Content/Templates/IAMetaAndRouter";
import {
    supportDirectories,
    SupportDirectoryTools,
} from "../../content/support";

const pageProps: IACardGridProps = {
    title: SupportDirectoryTools.title,
    subtitle: SupportDirectoryTools.subtitle,
    directoriesToRender: supportDirectories,
};
/** This is our main page content */
export const SupportCardGrid = () => <IACardGridTemplate {...pageProps} />;

const rootProps: IAMetaAndRouterProps = {
    path: SupportDirectoryTools.root,
    pageName: SupportDirectoryTools.title,
    indexComponent: SupportCardGrid,
    directoriesToRoute: supportDirectories,
};
/** Use this component in the main App Router! It will handle rendering everything
 * and set the Helmet values */
export const Support = () => <IAMetaAndRouter {...rootProps} />;
