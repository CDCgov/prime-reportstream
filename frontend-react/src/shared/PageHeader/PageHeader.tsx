import React from "react";
import { Breadcrumb, BreadcrumbBar } from "@trussworks/react-uswds";

import { USCrumbLink } from "../../components/USLink";
import { CallToAction } from "../../layouts/Markdown/CallToAction";

export interface PageHeaderProps {
    breadcrumbs?: Array<{ label: string; href: string }>;
    title?: string;
    subtitleArr?: string[];
    callToAction?: Array<{ label: string; href: string }>;
    lastUpdated?: string;
    isPrimaryDarker?: boolean;
}

export default function PageHeader({
    title,
    breadcrumbs,
    subtitleArr,
    callToAction,
    lastUpdated,
    isPrimaryDarker,
}: PageHeaderProps) {
    const stylePrimaryDarker = isPrimaryDarker
        ? "bg-primary-darker text-white"
        : "";
    return (
        <header
            className={`${stylePrimaryDarker} padding-y-9 margin-top-neg-5`}
        >
            <div className="grid-container">
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
                <h1 className="font-sans-3xl margin-y-2">{title}</h1>
                {subtitleArr?.map((s) => (
                    <p
                        key={s.slice(0, 5)}
                        className="font-sans-lg margin-top-4"
                    >
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
            </div>
        </header>
    );
}
