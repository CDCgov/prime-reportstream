import React from "react";
import {
    Link as RouteLink,
    NavLink as OrigNavLink,
    useLocation,
} from "react-router-dom";
import classnames from "classnames";
import { ButtonProps } from "@trussworks/react-uswds/lib/components/Button/Button";
import DOMPurify from "dompurify";
import { DefaultLinkProps } from "@trussworks/react-uswds/lib/components/Link/Link";

import site from "../../content/site.json";
import Icon from "../Icon/Icon";

import { getHrefRoute, isExternalUrl } from "./utils";
import styles from "./Link.module.scss";

export interface LinkProps extends Omit<DefaultLinkProps, "variant" | "href"> {
    state?: any;
    href?: string;
    variant?: DefaultLinkProps["variant"] | "breadcrumb";
    button?: boolean | Omit<ButtonProps, "type" | "children">;
}

export function LinkBase({
    className,
    href,
    state,
    variant,
    ref,
    button,
    children,
    ...props
}: LinkProps) {
    const sanitizedHref = href ? DOMPurify.sanitize(href) : href;
    const routeHref = getHrefRoute(sanitizedHref);
    const isFile = sanitizedHref?.startsWith("/assets/");
    const [assetName, assetInfo] = isFile
        ? Object.entries(site.assets as object).find(
              ([_, v]) => v.path === href,
          ) ?? []
        : [];
    const linkClassname =
        variant === "unstyled"
            ? classnames(styles.Link, className)
            : classnames(
                  styles.Link,
                  "usa-link",
                  {
                      "usa-link--external": variant === "external",
                      "usa-nav__link": variant === "nav",
                      "usa-breadcrumb__link": variant === "breadcrumb",
                  },
                  className,
              );
    const buttonClassname = button
        ? classnames(
              "usa-button",
              typeof button !== "boolean" && {
                  "usa-button--secondary": button.secondary,
                  [`usa-button--accent-${button.accentStyle}`]:
                      button.accentStyle,
                  "usa-button--base": button.base,
                  "usa-button--outline": button.outline,
                  "usa-button--inverse": button.inverse,
                  [`usa-button--${button.size}`]: button.size,
                  "usa-button--unstyled": button.unstyled,
              },
              className,
          )
        : "";
    const classname = classnames(linkClassname, buttonClassname);
    const extraProps = {
        ...(variant === "external"
            ? {
                  target: "_blank",
                  rel: "noreferrer noopener",
              }
            : {}),
        download: isFile,
        "data-id": assetInfo ? assetName : undefined,
        "data-parentid": assetInfo?.tracking?.parentId,
        children: isFile ? (
            <>
                {children}
                <Icon name="FileDownload" className="usa-icon--auto" />
            </>
        ) : (
            children
        ),
    };

    if (routeHref !== undefined && !isFile) {
        const routeProps = {
            to: routeHref,
            state,
            className: classname,
            ...extraProps,
            ...props,
        };
        if (variant !== "nav") {
            return <RouteLink {...routeProps} />;
        } else {
            return (
                <NavLink
                    {...routeProps}
                    className={({ isActive }) =>
                        classnames(classname, { "usa-current": isActive })
                    }
                />
            );
        }
    }

    return (
        // eslint-disable-next-line jsx-a11y/anchor-has-content
        <a
            href={sanitizedHref}
            className={classname}
            ref={ref}
            {...extraProps}
            {...props}
        />
    );
}

/**
 * Replacement component for react-uswds' Link. Supports breadcrumb
 * as additional variant. Will render with button class(es) if
 * button prop provided.
 *
 * Also:
 * - sanitizes href
 * - automatically determines if external
 * - automatically determines if route or regular link
 * - automatically determines if site file
 * - passes nav variant to our NavLink proxy
 * - automatically attaches AppInsight tracking data attributes
 *   if specified for file in site.json
 */
export function Link(props: LinkProps) {
    const variant =
        props.variant ?? isExternalUrl(props.href)
            ? ("external" as const)
            : undefined;
    return <LinkBase variant={variant} {...props} />;
}

export interface NavLinkProps
    extends React.ComponentProps<typeof OrigNavLink> {}

/**
 * NavLink (react-router-dom) proxy with additional logic for determining
 * activeness. Logic not merged with Link in order to keep Link component
 * as static as possible (otherwise all links would rerender on location change).
 */
export function NavLink({ to, className, ...props }: NavLinkProps) {
    const { hash: currentHash } = useLocation();
    const href = to as string;
    const hashIndex = href?.indexOf("#") ?? -1;
    const hash = hashIndex > -1 ? href?.slice(hashIndex) : "";

    return (
        <OrigNavLink
            to={to}
            className={({ isActive: isPathnameActive, isPending }) => {
                // Without this, all hash links would be considered active for a path
                const isActive =
                    isPathnameActive && (hash === "" || currentHash === hash);
                const classname =
                    typeof className === "function"
                        ? className({ isActive, isPending })
                        : className;
                return classname;
            }}
            {...props}
        />
    );
}
