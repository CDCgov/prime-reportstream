import { useCallback } from "react";

import { ContentDirectory } from "../MarkdownDirectory";

import { IACardGridTemplate } from "./IACardGridTemplate";
import IASideNavTemplate from "./IASideNavTemplate";
import { IAMeta, IARouter } from "./IAMeta";

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
    /* For use in cases where the template doesn't have routing built in
     * (for example, in `IASideNavTemplate`) */
    includeRouter?: boolean;
    /* Directories accompanying your index. */
    directories?: ContentDirectory[];
}
/** Takes in props to hydrate and render the right template */
export const IATemplate = ({
    pageName,
    subtitle,
    templateKey,
    templateProps,
    includeRouter,
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
            <IAMeta pageName={pageName} />
            <div className="rs-hero__index">
                <div className="grid-container">
                    <h1>{pageName}</h1>
                    <h2>{subtitle}</h2>
                </div>
            </div>
            {/* In cases where your template index doesn't include a router,
             we provide the IARouter component, and use your template as the
             index of this section */}
            {includeRouter && directories ? (
                <IARouter
                    indexElement={template(templateKey)}
                    directories={directories}
                />
            ) : (
                /* When your template has its own routing, we just render the
                 * template */
                template(templateKey)
            )}
        </>
    );
};
