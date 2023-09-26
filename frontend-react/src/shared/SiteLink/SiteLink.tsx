import { Link, LinkProps } from "../Link/Link";

import { SiteItems, getSiteItemHref } from "./utils";

export interface SiteLinkProps extends Omit<LinkProps, "href"> {
    name: SiteItems;
}

/**
 * Shortcut component that proxies Link to render specific entries in site.json as
 * links. Will automatically find whatever is set as url/href/path. If requesting
 * an email, it will autoformat as mailto.
 */
export function SiteLink({ name, ...props }: SiteLinkProps) {
    let href = getSiteItemHref(name);

    if (!href || typeof href !== "string")
        throw new Error(`Could not determine href for ${name}.`);

    if (name.endsWith(".email")) {
        href = `mailto:${href}`;
    }

    return <Link href={href} {...props} />;
}

export default SiteLink;
