import site from "../../content/site.json";

type SiteType = typeof site;

type OrgsEmail = {
    [k in keyof SiteType["orgs"]]: "email" extends keyof SiteType["orgs"][k]
        ? SiteType["orgs"][k]["email"] extends string
            ? SiteType["orgs"][k]
            : never
        : never;
};

export type SiteItems =
    | `forms.${keyof SiteType["forms"]}`
    | `assets.${keyof SiteType["assets"]}`
    | `orgs.${keyof SiteType["orgs"]}`
    | `orgs.${keyof OrgsEmail}.email`;

export function getSiteItemHref(name: string, s: object = site) {
    const drilldownProps = name.split(".");
    return drilldownProps.reduce((p, c, i, arr) => {
        let nextProp = p[c];

        if (typeof nextProp !== "string" && i + 1 === arr.length) {
            nextProp = nextProp.url || nextProp.href || nextProp.path;
        }

        return nextProp;
    }, s as any);
}
