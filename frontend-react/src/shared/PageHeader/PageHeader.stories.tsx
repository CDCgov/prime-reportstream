// AutoUpdateFileChromatic
import React from "react";

import PageHeader from "./PageHeader";

export default {
    title: "components/PageHeader",
    component: PageHeader,
};

export const RSPageHeader = (): React.ReactElement => (
    <PageHeader
        title={"This is the title"}
        subtitleArr={["This is the subtitle"]}
        callToAction={[
            {
                href: "https://app.smartsheetgov.com/b/form/48f580abb9b440549b1a9cf996ba6957",
                label: "Connect now",
            },
        ]}
    />
);

export const RSFeatureWithBreadcrumb = (): React.ReactElement => (
    <PageHeader
        title={"This is the title"}
        subtitleArr={["This is the subtitle"]}
        breadcrumbs={[
            { href: "", label: "Homepage" },
            { href: "", label: "Sub Page" },
        ]}
        lastUpdated={"2022-11-07T22:56:07.832+00:00"}
    />
);
