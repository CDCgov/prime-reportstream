/**
 * Try to parse the href as a URL. If it throws, then it's not
 * an absolute href (aka is internal). If it parses, verify it is
 * from the cdc.gov domain (aka is internal).
 */
export function isExternalUrl(href?: string) {
    if (href === undefined) return false;
    try {
        // Browsers allow // shorthand in anchor urls but URL does not
        const url = new URL(
            href.replace(/^\/\//, `${window.location.protocol}//`),
        );
        return (
            (url.protocol.startsWith("http") &&
                url.host !== "cdc.gov" &&
                !url.host.endsWith(".cdc.gov")) ||
            href.startsWith("mailto:")
        );
    } catch (e: any) {
        return false;
    }
}

/**
 * Stateless function to get route href from href that could be
 * absolute, shorthand, relative, and/or a non-route.
 * Attempt to parse href as URL (taking into account "//" shorthand).
 * If it errors, then assume its a relative url (aka route). If it
 * parses, then verify its an absolute route through origins.
 *
 * If href is a hash anchor link, return undefined so as to bypass
 * passing through react-router.
 */
export function getHrefRoute(href?: string): string | undefined {
    if (href === undefined) return undefined;

    try {
        const url = new URL(
            href.replace(/^\/\//, `${window.location.protocol}//`),
        );
        if (
            url.protocol.startsWith("http") &&
            url.origin === window.location.origin
        )
            return `${url.pathname}${url.search}`;
    } catch (e: any) {
        return href;
    }

    return undefined;
}
