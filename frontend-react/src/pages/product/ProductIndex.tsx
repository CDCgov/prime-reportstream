import React from "react";

import { IASideNavProps } from "../../components/Content/Templates/IASideNavTemplate";
import {
    IATemplate,
    IATemplateProps,
    TemplateName,
} from "../../components/Content/Templates/IATemplate";
import {
    productDirectories,
    ProductDirectoryTools,
} from "../../content/product";

const rootProps: IATemplateProps<IASideNavProps> = {
    pageName: ProductDirectoryTools.title,
    subtitle: ProductDirectoryTools.subtitle,
    templateKey: TemplateName.SIDE_NAV,
    templateProps: {
        directories: productDirectories,
        rootRedirect: "/product/overview",
    },
};
export const Product = () => <IATemplate {...rootProps} />;
