import React, { PropsWithChildren } from "react";
import { NavLink } from "react-router-dom";

// Known issue with the `PropsWithChildren generic in React 18,
// so I wrote this in a way that we can just remove `<any>` and be
// okay if we update our React version.
interface USNavLinkProps extends PropsWithChildren<any> {
    href: string;
    className?: string;
    activeClassName?: string;
}
type USLinkProps = Omit<USNavLinkProps, "activeClassName">;

/** A single link for rendering standard links */
export const USLink = ({ children, href, className }: USLinkProps) => (
    <a href={href} className={`usa-link ${className}`}>
        {children}
    </a>
);

/** A single link for rendering external links */
export const USExtLink = ({ href, className, children }: USLinkProps) => (
    <USLink href={href} className={`usa-link--external ${className}`}>
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
    const navLinkStyles = ["usa-nav__link"];
    // Extracted functionality for active vs standard classNames
    const getStyles = (isActive: boolean) => {
        if (isActive) {
            navLinkStyles.push(activeClassName || "");
        } else {
            navLinkStyles.push(className || "");
        }
        return navLinkStyles.join(" ");
    };
    return (
        <NavLink to={href} className={({ isActive }) => getStyles(isActive)}>
            {children}
        </NavLink>
    );
};
