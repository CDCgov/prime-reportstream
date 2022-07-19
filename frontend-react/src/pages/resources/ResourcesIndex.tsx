import { Helmet } from "react-helmet";
import { Route, Switch } from "react-router-dom";

import {
    ElementDirectory,
    GeneratedRouter,
} from "../../components/Markdown/MarkdownDirectory";
import ReferralGuide from "../../content/resources/referral-guide.md";
import { MarkdownContent } from "../../components/Markdown/MarkdownContent";
import {
    contentContainer,
    ContentDirectoryTools,
    SlugParams,
} from "../../components/Markdown/DirectoryGenerationTools";
import { IACardList } from "../../components/IACard";

import ProgrammersGuide from "./programmers-guide/ProgrammersGuide";
import { AccountRegistrationGuideIa } from "./AccountRegistrationGuide";
import { CsvUploadGuideIa } from "./CsvUploadGuide";
import { DataDownloadGuideIa } from "./DataDownloadGuide";
import { ELRChecklistIa } from "./ElrChecklist";
import { SystemAndSettingsIa } from "./SystemAndSettings";
import { SecurityPracticesIa } from "./SecurityPractices";
import { GettingStartedPhd } from "./GettingStartedPhd";

/** Data that drives breadcrumb creation and slug appending
 * @todo: Refactor to make easier for content/design to create */
const slugs: SlugParams[] = [
    {
        key: "ACCOUNT_REGISTRATION",
        slug: "account-registration-guide",
    },
    { key: "UPLOAD_GUIDE", slug: "csv-upload-guide" },
    { key: "DOWNLOAD_GUIDE", slug: "data-download-guide" },
    { key: "REFERRAL_GUIDE", slug: "referral-guide" },
    { key: "PROGRAMMERS_GUIDE", slug: "programmers-guide" },
    { key: "ELR_CHECKLIST", slug: "elr-checklist" },
    { key: "SYSTEM", slug: "system-and-settings" },
    { key: "SECURITY", slug: "security-practices" },
    {
        key: "GETTING_STARTED_PHD",
        slug: "getting-started-public-health-departments",
    },
];

/* Tools to help generate Directories */
const DirectoryTools = new ContentDirectoryTools()
    .setName("Resources")
    .setRoot("/resources")
    .setSlugs(slugs);

/* An array of directories to be rendered */
const directories = [
    new ElementDirectory(
        "Account registration guide",
        DirectoryTools.prependRoot("ACCOUNT_REGISTRATION"),
        "Step-by-step instructions for setting up a new user account.",
        contentContainer(
            <AccountRegistrationGuideIa />,
            DirectoryTools.makeCrumb("Account Registration Guide")
        )
    ),
    new ElementDirectory(
        "Getting started: Public health departments",
        DirectoryTools.prependRoot("GETTING_STARTED_PHD"),
        "foo foo getting started for PHDs.",
        contentContainer(
            <GettingStartedPhd />,
            DirectoryTools.makeCrumb(
                "Getting started: Public health departments"
            )
        )
    ),
    new ElementDirectory(
        "ELR onboarding checklist",
        DirectoryTools.prependRoot("ELR_CHECKLIST"),
        "Checklist to help gather information to submit.",
        contentContainer(
            <ELRChecklistIa />,
            DirectoryTools.makeCrumb("ELR Checklist")
        )
    ),
    new ElementDirectory(
        "Programmer's guide",
        DirectoryTools.prependRoot("PROGRAMMERS_GUIDE"),
        "Instructions and tools for testing facilities and organizations reporting data via the ReportStream Restful (REST) API.",
        contentContainer(
            <ProgrammersGuide />,
            DirectoryTools.makeCrumb("Programmer's Guide")
        )
    ),
    new ElementDirectory(
        "CSV upload guide",
        DirectoryTools.prependRoot("UPLOAD_GUIDE"),
        "Instructions for testing facilities and organizations reporting data via comma separated values (CSV).",
        contentContainer(
            <CsvUploadGuideIa />,
            DirectoryTools.makeCrumb("CSV Upload Guide")
        )
    ),
    new ElementDirectory(
        "CSV download guide",
        DirectoryTools.prependRoot("DOWNLOAD_GUIDE"),
        "Instructions for public health departments to download data as comma separated values (CSV).",
        contentContainer(
            <DataDownloadGuideIa />,
            DirectoryTools.makeCrumb("Data Download Guide")
        )
    ),
    new ElementDirectory(
        "ReportStream referral guide",
        DirectoryTools.prependRoot("REFERRAL_GUIDE"),
        "Instructions and templates for public health departments to onboard reporting entities in their jurisdiction.",
        contentContainer(
            <MarkdownContent markdownUrl={ReferralGuide} />,
            DirectoryTools.makeCrumb("ReportStream referral guide")
        )
    ),
    new ElementDirectory(
        "System and settings",
        DirectoryTools.prependRoot("SYSTEM"),
        "Information about the ReportStream platform, including data configuration, formats, and transport.",
        contentContainer(
            <SystemAndSettingsIa />,
            DirectoryTools.makeCrumb("System and Settings")
        )
    ),
    new ElementDirectory(
        "Security practices",
        DirectoryTools.prependRoot("SECURITY"),
        "Answers to common questions about ReportStream security and data practices.",
        contentContainer(
            <SecurityPracticesIa />,
            DirectoryTools.makeCrumb("Security Practices")
        )
    ),
];

/** Main render source for Resources IA page -- provides router
 * @todo: Extract as general purpose component since it's reused across IA */
export const Resources = () => {
    const path = "/resources";
    return (
        <>
            <Helmet>
                <title>Resources | {process.env.REACT_APP_TITLE}</title>
            </Helmet>

            <Switch>
                <Route exact path={path} component={ResourcesIndex} />
                <GeneratedRouter directories={directories} />
            </Switch>
        </>
    );
};

/** @todo: Extract as general purpose component since it's reused across IA*/
export const ResourcesIndex = () => {
    return (
        <>
            <div className="rs-hero__index">
                <div className="grid-container">
                    <h1>Resources</h1>
                    <h2>Explore guides, tools, and documentation</h2>
                </div>
            </div>
            <div className="grid-container usa-prose margin-top-6">
                <div className="grid-row grid-gap">
                    <section>
                        <IACardList dirs={directories} />
                    </section>
                </div>
            </div>
        </>
    );
};
