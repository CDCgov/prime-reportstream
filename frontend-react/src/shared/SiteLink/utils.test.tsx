import { getSiteItemHref } from "./utils";

describe("SiteLink utils", () => {
    const site = {
        orgs: {
            testOrg: {
                url: "testOrg.com",
            },
            testOrg2: {
                email: "test@testOrg2.com",
                url: "testOrg2.com",
            },
        },
        forms: {
            enquire: {
                url: "https://somesite.com/formabc",
            },
        },
        assets: {
            flyer: {
                path: "/assets/pdf/flyer.pdf",
            },
        },
    };
    const names = [
        { name: "orgs.testOrg", expectedHref: site.orgs.testOrg.url },
        { name: "orgs.testOrg2", expectedHref: site.orgs.testOrg2.url },
        { name: "orgs.testOrg2.email", expectedHref: site.orgs.testOrg2.email },
        { name: "forms.enquire", expectedHref: site.forms.enquire.url },
        { name: "assets.flyer", expectedHref: site.assets.flyer.path },
    ];

    test.each(names)("getSiteItemHref with %s", ({ name, expectedHref }) => {
        expect(getSiteItemHref(name, site)).toStrictEqual(expectedHref);
    });
});
