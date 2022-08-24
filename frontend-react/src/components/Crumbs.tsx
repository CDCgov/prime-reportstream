import {
    BreadcrumbBar,
    Breadcrumb,
    BreadcrumbLink,
} from "@trussworks/react-uswds";
import { ReactChild } from "react";

interface CrumbConfig {
    label: string;
    path?: string;
}

interface CrumbsProps {
    crumbList?: CrumbConfig[];
    noPadding?: boolean;
}

const Crumbs = ({ crumbList, noPadding }: CrumbsProps) => {
    if (crumbList) {
        return (
            <div className={!noPadding ? "grid-container" : ""}>
                <BreadcrumbBar>
                    {crumbList?.map((crumbConfig) => (
                        <Breadcrumb
                            key={`${crumbConfig.label}`}
                            current={!crumbConfig?.path}
                        >
                            {crumbConfig?.path ? (
                                <BreadcrumbLink href={crumbConfig?.path || ""}>
                                    {crumbConfig.label}
                                </BreadcrumbLink>
                            ) : (
                                crumbConfig.label
                            )}
                        </Breadcrumb>
                    ))}
                </BreadcrumbBar>
            </div>
        );
    } else {
        return (
            <div className="grid-container margin-top-5">
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
