import React from "react";
import { Breadcrumb, BreadcrumbBar } from "@trussworks/react-uswds";

import { USCrumbLink } from "../../components/USLink";
import { CallToAction } from "../../layouts/Markdown/CallToAction";

export type PageHeaderProps = React.PropsWithChildren<
    {
        breadcrumbs?: Array<{ label: string; href: string }>;
        title?: string;
        subtitleArr?: string[];
        callToAction?: Array<{ label: string; href: string }>;
        lastUpdated?: string;
    } & React.HTMLAttributes<HTMLElement>
>;

export function PageHeader({
    title,
    breadcrumbs,
    subtitleArr,
    callToAction,
    lastUpdated,
    ...props
}: PageHeaderProps) {
    return (
        <header {...props}>
            {breadcrumbs != null ? (
                <BreadcrumbBar>
                    {breadcrumbs.map((b) => (
                        <Breadcrumb key={b.label}>
                            {b.href ? (
                                <USCrumbLink href={b.href}>
                                    {b.label}
                                </USCrumbLink>
                            ) : (
                                b.label
                            )}
                        </Breadcrumb>
                    ))}
                </BreadcrumbBar>
            ) : null}
            <h1>{title}</h1>
            {subtitleArr?.map((s) => (
                <p key={s.slice(0, 5)} className="rs-subtitle">
                    {s}
                </p>
            ))}
            {(callToAction || lastUpdated) && (
                <div className="grid-row margin-top-8 margin-bottom-2">
                    {callToAction?.map((c) => (
                        <CallToAction key={c.label} {...c} />
                    ))}
                    {lastUpdated && (
                        <p className="text-base text-italic">
                            Last updated: {lastUpdated}
                        </p>
                    )}
                </div>
            )}
        </header>
    );
}

export default PageHeader;
