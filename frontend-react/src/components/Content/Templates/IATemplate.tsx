import { useCallback } from "react";

import { ContentDirectory } from "../MarkdownDirectory";

import { IACardGridTemplate } from "./IACardGridTemplate";
import IASideNavTemplate from "./IASideNavTemplate";
import { IAMetaAndRouter } from "./IAMetaAndRouter";

/** Template names! Add the universal template key here whenever
 * you make a new template. */
export enum TemplateName {
    CARD_GRID = "card-grid",
    SIDE_NAV = "side-nav",
}

export interface IATemplateProps<P> {
    pageName: string;
    subtitle: string;
    templateKey: TemplateName;
    templateProps: P;
    directories: ContentDirectory[];
}
/** Takes in props to hydrate and render the right template */
export const IATemplate = ({
    pageName,
    subtitle,
    templateKey,
    templateProps,
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
            <div className="rs-hero__index">
                <div className="grid-container">
                    <h1>{pageName}</h1>
                    <h2>{subtitle}</h2>
                </div>
            </div>
            {template(templateKey)}
        </>
    );
};
