import { useCallback } from "react";

import { ContentDirectory } from "../MarkdownDirectory";

import { IACardGridTemplate } from "./IACardGridTemplate";
import IASideNavTemplate from "./IASideNavTemplate";
import { IAMetaAndRouter } from "./IAMetaAndRouter";

export enum TemplateName {
    CARD_GRID = "card-grid",
    SIDE_NAV = "side-nav",
}

export interface IATemplateProps<P> {
    pageName: string;
    templateKey: TemplateName;
    templateProps: P;
    directories: ContentDirectory[];
}

export const IATemplate = ({
    templateProps,
    pageName,
    templateKey,
    directories,
}: IATemplateProps<any>) => {
    const template = useCallback((key: TemplateName) => {
        switch (key) {
            case TemplateName.CARD_GRID:
                return <IACardGridTemplate {...templateProps} />;
            case TemplateName.SIDE_NAV:
                return <IASideNavTemplate {...templateProps} />;
        }
    }, []); // eslint-disable-line
    return (
        <>
            <IAMetaAndRouter directories={directories} pageName={pageName} />
            {template(templateKey)}
        </>
    );
};
