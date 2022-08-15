import { Helmet } from "react-helmet";
import { Route, Switch } from "react-router-dom";

import {
    ElementDirectory,
    GeneratedRouter,
} from "../../components/Content/MarkdownDirectory";
import ReferralGuide from "../../content/resources/referral-guide.md";
import { MarkdownContent } from "../../components/Content/MarkdownContent";
import {
    contentContainer,
    ContentDirectoryTools,
    SlugParams,
} from "../../components/Content/PageGenerationTools";
import { IACardList } from "../../components/IACard";

import ProgrammersGuide from "./programmers-guide/ProgrammersGuide";
import { AccountRegistrationGuideIa } from "./AccountRegistrationGuide";
import { CsvSchemaDocumentation } from "./CsvSchemaDocumentation";
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
    { key: "SCHEMA_DOCUMENTATION", slug: "csv-schema" },
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
    new ElementDirectory()
        .setTitle("Account registration guide")
        .setSlug(DirectoryTools.prependRoot("ACCOUNT_REGISTRATION"))
        .setDescription(
            "Step-by-step instructions for setting up a new user account."
        )
        .addElement(
            contentContainer(
                <AccountRegistrationGuideIa />,
                DirectoryTools.makeCrumb("Account registration guide")
            )
        ),
    new ElementDirectory()
        .setTitle("Getting started: Public health departments")
        .setSlug(DirectoryTools.prependRoot("GETTING_STARTED_PHD"))
        .setDescription(
            "Step-by-step process for connecting your jurisdiction to ReportStream."
        )
        .addElement(
            contentContainer(
                <GettingStartedPhd />,
                DirectoryTools.makeCrumb(
                    "Getting started: public health departments"
                )
            )
        ),
    new ElementDirectory()
        .setTitle("ELR onboarding checklist")
        .setSlug(DirectoryTools.prependRoot("ELR_CHECKLIST"))
        .setDescription(
            "Checklist of required information for public health departments to set up an ELR connection."
        )
        .addElement(
            contentContainer(
                <ELRChecklistIa />,
                DirectoryTools.makeCrumb("ELR onboarding checklist")
            )
        ),
    new ElementDirectory()
        .setTitle("API Programmer's guide")
        .setSlug(DirectoryTools.prependRoot("PROGRAMMERS_GUIDE"))
        .setDescription(
            "Checklist of requirements for  setting up an ELR connection at your public health department."
        )
        .addElement(
            contentContainer(
                <ProgrammersGuide />,
                DirectoryTools.makeCrumb("API Programmer's guide")
            )
        ),
    new ElementDirectory()
        .setTitle("CSV schema documentation guide")
        .setSlug(DirectoryTools.prependRoot("SCHEMA_DOCUMENTATION"))
        .setDescription(
            "General formatting guidelines and data elements guidance  for CSV upload submissions."
        )
        .addElement(
            contentContainer(
                <CsvSchemaDocumentation />,
                DirectoryTools.makeCrumb("CSV schema documentation")
            )
        ),
    new ElementDirectory()
        .setTitle("CSV upload guide")
        .setSlug(DirectoryTools.prependRoot("UPLOAD_GUIDE"))
        .setDescription(
            "Instructions for testing facilities and organizations reporting data via comma separated values (CSV)."
        )
        .addElement(
            contentContainer(
                <CsvUploadGuideIa />,
                DirectoryTools.makeCrumb("CSV upload guide")
            )
        ),
    new ElementDirectory()
        .setTitle("CSV download guide")
        .setSlug(DirectoryTools.prependRoot("DOWNLOAD_GUIDE"))
        .setDescription(
            "Instructions for downloading data as comma separated values (CSV) for your public health department."
        )
        .addElement(
            contentContainer(
                <DataDownloadGuideIa />,
                DirectoryTools.makeCrumb("CSV download guide")
            )
        ),
    new ElementDirectory()
        .setTitle("ReportStream referral guide")
        .setSlug(DirectoryTools.prependRoot("REFERRAL_GUIDE"))
        .setDescription(
            "Instructions and templates for referring reporting entities to use ReportStream in your jurisdiction."
        )
        .addElement(
            contentContainer(
                <MarkdownContent markdownUrl={ReferralGuide} />,
                DirectoryTools.makeCrumb("ReportStream referral guide")
            )
        ),
    new ElementDirectory()
        .setTitle("System and settings")
        .setSlug(DirectoryTools.prependRoot("SYSTEM"))
        .setDescription(
            "Information about the ReportStream platform, including data storage, configuration, formatting, transport. "
        )
        .addElement(
            contentContainer(
                <SystemAndSettingsIa />,
                DirectoryTools.makeCrumb("System and settings")
            )
        ),
    new ElementDirectory()
        .setTitle("Security practices")
        .setSlug(DirectoryTools.prependRoot("SECURITY"))
        .setDescription(
            "Answers to common questions about ReportStream security and data practices."
        )
        .addElement(
            contentContainer(
                <SecurityPracticesIa />,
                DirectoryTools.makeCrumb("Security practices")
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
                    <h2>
                        Explore guides, tools, and resources to optimize
                        ReportStream{" "}
                    </h2>
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
