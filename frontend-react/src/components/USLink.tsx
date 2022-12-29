import React, { AnchorHTMLAttributes, PropsWithChildren } from "react";
import { NavLink } from "react-router-dom";

// Known issue with the `PropsWithChildren generic in React 18,
// so I wrote this in a way that we can just remove `<any>` and be
// okay if we update our React version.
type USNavLinkProps = PropsWithChildren<any> &
    AnchorHTMLAttributes<any> & { className?: string };
type USLinkProps = Omit<USNavLinkProps, "activeClassName">;

/** A single link for rendering standard links */
export const USLink = ({
    children,
    href,
    className,
    rel,
    target,
}: USLinkProps) => (
    <a
        target={target}
        rel={rel}
        href={href}
        className={`usa-link ${className}`}
    >
        {children}
    </a>
);

/** A single link for rendering external links */
export const USExtLink = ({ href, className, children }: USLinkProps) => (
    <USLink
        target="_blank"
        rel="noreferrer noopener"
        href={href}
        className={`usa-link--external ${className}`}
    >
        {children}
    </USLink>
);

/** A single link for building breadcrumbs */
export const USCrumbLink = ({ href, className, children }: USLinkProps) => (
    <USLink href={href} className={`usa-breadcrumb__link ${className}`}>
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
            to={href}
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
