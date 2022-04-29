import {
    BreadcrumbBar,
    Breadcrumb,
    BreadcrumbLink,
} from "@trussworks/react-uswds";

interface CrumbConfig {
    label: string;
    path?: string;
}

interface CrumbsProps {
    crumbList?: CrumbConfig[];
}

const Crumbs = ({ crumbList }: CrumbsProps) => {
    if (crumbList) {
        return (
            <div className="grid-container">
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

export default Crumbs;
export type { CrumbsProps, CrumbConfig };
