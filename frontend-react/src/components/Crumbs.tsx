import { BreadcrumbBar, Breadcrumb, Icon } from "@trussworks/react-uswds";
import { ReactChild } from "react";

import { IconButton } from "./IconButton";
import { USCrumbLink } from "./USLink";

interface CrumbConfig {
    label: string;
    path?: string;
}

interface CrumbsProps {
    crumbList?: CrumbConfig[];
    previousPage?: string;
}

const Crumbs = ({ crumbList, previousPage }: CrumbsProps) => {
    if (crumbList || previousPage) {
        return (
            <div>
                {Boolean(previousPage) ? (
                    <div className="font-sans-lg line-height-sans-4 margin-top-8">
                        <IconButton
                            size="big"
                            type="button"
                            onClick={() => window.history.back()}
                        >
                            <Icon.ArrowBack />
                        </IconButton>
                        Return to {previousPage}
                    </div>
                ) : null}
                {crumbList !== undefined && crumbList.length > 0 ? (
                    <>
                        <BreadcrumbBar>
                            {crumbList?.map((crumbConfig) => (
                                <Breadcrumb
                                    key={`${crumbConfig.label}`}
                                    current={!crumbConfig?.path}
                                >
                                    {crumbConfig?.path ? (
                                        <USCrumbLink
                                            href={crumbConfig?.path || ""}
                                        >
                                            {crumbConfig.label}
                                        </USCrumbLink>
                                    ) : (
                                        crumbConfig.label
                                    )}
                                </Breadcrumb>
                            ))}
                        </BreadcrumbBar>
                    </>
                ) : null}
            </div>
        );
    } else {
        return (
            <div className="margin-top-5">
                <span>No crumbs given</span>
            </div>
        );
    }
};

interface WithCrumbsProps extends CrumbsProps {
    page: ReactChild;
}
/** HOC to provide Crumbs at top of page
 * @param props {WithCrumbsProps} Pass in an array of CrumbConfigs for rendering and
 * a page to render with it */
export const WithCrumbs = ({ crumbList, page }: WithCrumbsProps) => {
    return (
        <>
            <Crumbs crumbList={crumbList} />
            {page}
        </>
    );
};

export default Crumbs;
export type { CrumbsProps, CrumbConfig };
