import { useMemo } from "react";

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
}
/** Takes in props to hydrate and render the right template */
export const IATemplate = ({
    pageName,
    subtitle,
    templateKey,
    templateProps,
    includeRouter,
}: IATemplateProps<any>) => {
    const template = useMemo(() => {
        switch (templateKey) {
            case TemplateName.CARD_GRID:
                return (
                    <IACardGridTemplate
                        pageName={pageName}
                        subtitle={subtitle}
                        {...templateProps}
                    />
                );
            case TemplateName.SIDE_NAV:
                return <IASideNavTemplate {...templateProps} />;
        }
    }, []); // eslint-disable-line
    return (
        <>
            <IAMeta pageName={pageName} />
            {/* In cases where your template index doesn't include a router,
             we provide the IARouter component, and use your template as the
             index of this section */}
            {includeRouter && templateProps.directories ? (
                <IARouter
                    indexElement={template}
                    directories={templateProps.directories}
                />
            ) : (
                /* When your template has its own routing, we just render the
                 * template */
                template
            )}
        </>
    );
};
