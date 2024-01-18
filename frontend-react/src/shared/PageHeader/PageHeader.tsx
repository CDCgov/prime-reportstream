import React, { HTMLAttributes, PropsWithChildren } from "react";
import { Breadcrumb, BreadcrumbBar } from "@trussworks/react-uswds";

import { USCrumbLink, USLinkButton } from "../../components/USLink";

export type PageHeaderProps = PropsWithChildren<
    {
        breadcrumbs?: Array<{ label: string; href: string }>;
        title?: string;
        subtitleArr?: string[];
        callToAction?: Array<{ label: string; href: string }>;
        lastUpdated?: string;
    } & HTMLAttributes<HTMLElement>
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
                        <USLinkButton key={c.label} href={c.href}>
                            {c.label}
                        </USLinkButton>
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
