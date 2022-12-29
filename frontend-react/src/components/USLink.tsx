import React, { AnchorHTMLAttributes } from "react";
import { Link, NavLink } from "react-router-dom";

// Known issue with the `PropsWithChildren generic in React 18,
// so I wrote this in a way that we can just remove `<any>` and be
// okay if we update our React version.
interface CustomLinkProps {
    children: React.ReactNode | React.ReactNode[];
    anchor?: boolean;
    className?: string;
    activeClassName?: string;
}
type USLinkProps = AnchorHTMLAttributes<{}> &
    Omit<CustomLinkProps, "activeClassName">;
type USNavLinkProps = Pick<AnchorHTMLAttributes<{}>, "href"> & CustomLinkProps;

/** A single link for rendering standard links */
export const USLink = ({
    anchor = false,
    children,
    className,
    href,
    ...anchorHTMLAttributes
}: USLinkProps) => {
    return !anchor ? (
        <Link
            to={href || ""}
            className={`usa-link ${className}`}
            {...anchorHTMLAttributes}
        >
            {children}
        </Link>
    ) : (
        <a
            href={href}
            className={`usa-link ${className}`}
            {...anchorHTMLAttributes}
        >
            {children}
        </a>
    );
};

/** A single link for rendering external links */
export const USExtLink = ({
    className,
    children,
    ...anchorHTMLAttributes
}: Omit<USLinkProps, "anchor" | "rel" | "target">) => (
    <USLink
        anchor
        target="_blank"
        rel="noreferrer noopener"
        className={`usa-link--external ${className}`}
        {...anchorHTMLAttributes}
    >
        {children}
    </USLink>
);

/** A single link for building breadcrumbs */
export const USCrumbLink = ({
    className,
    children,
    ...anchorHTMLAttributes
}: Omit<USLinkProps, "anchor">) => (
    <USLink
        className={`usa-breadcrumb__link ${className}`}
        {...anchorHTMLAttributes}
    >
        {children}
    </USLink>
);

/** A single link to replace NavLink (react-router-dom) */
export const USNavLink = ({
    href,
    children,
    className,
    activeClassName,
}: USNavLinkProps) => {
    return (
        <NavLink
            to={href || ""}
            className={({ isActive }) =>
                isActive
                    ? `usa-nav__link usa-current ${activeClassName || ""}`
                    : `usa-nav__link ${className || ""}`
            }
        >
            {children}
        </NavLink>
    );
};
