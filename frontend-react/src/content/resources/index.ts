/* An array of directories to be rendered */
import { ElementDirectory } from "../../components/Content/MarkdownDirectory";
import {
    contentContainer,
    ContentDirectoryTools,
    SlugParams,
} from "../../components/Content/PageGenerationTools";
import {
    AccountRegistrationGuideIa,
    GettingStartedPhd,
    ELRChecklistIa,
    ProgrammersGuide,
    CsvSchemaDocumentation,
    CsvUploadGuideIa,
    DataDownloadGuideIa,
    SystemAndSettingsIa,
    SecurityPracticesIa,
} from "../../pages/resources/index-legacy";
import { ReferralGuideMd } from "../../pages/resources/markdown-adapters";

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
export const ResourcesDirectoryTools = new ContentDirectoryTools()
    .setTitle("Resources")
    .setSubtitle(
        "Explore guides, tools, and resources to optimize ReportStream"
    )
    .setRoot("/resources")
    .setSlugs(slugs);

export const resourcesDirectories = [
    new ElementDirectory()
        .setTitle("Account registration guide")
        .setSlug(ResourcesDirectoryTools.prependRoot("ACCOUNT_REGISTRATION"))
        .setDescription(
            "Step-by-step instructions for setting up a new user account."
        )
        .addElement(
            contentContainer(
                AccountRegistrationGuideIa,
                ResourcesDirectoryTools.makeCrumb("Account registration guide")
            )
        ),
    new ElementDirectory()
        .setTitle("Getting started: Public health departments")
        .setSlug(ResourcesDirectoryTools.prependRoot("GETTING_STARTED_PHD"))
        .setDescription(
            "Step-by-step process for connecting your jurisdiction to ReportStream."
        )
        .addElement(
            contentContainer(
                GettingStartedPhd,
                ResourcesDirectoryTools.makeCrumb(
                    "Getting started: public health departments"
                )
            )
        ),
    new ElementDirectory()
        .setTitle("ELR onboarding checklist")
        .setSlug(ResourcesDirectoryTools.prependRoot("ELR_CHECKLIST"))
        .setDescription(
            "Checklist of required information for public health departments to set up an ELR connection."
        )
        .addElement(
            contentContainer(
                ELRChecklistIa,
                ResourcesDirectoryTools.makeCrumb("ELR onboarding checklist")
            )
        ),
    new ElementDirectory()
        .setTitle("API Programmer's guide")
        .setSlug(ResourcesDirectoryTools.prependRoot("PROGRAMMERS_GUIDE"))
        .setDescription(
            "Checklist of requirements for  setting up an ELR connection at your public health department."
        )
        .addElement(
            contentContainer(
                ProgrammersGuide,
                ResourcesDirectoryTools.makeCrumb("API Programmer's guide")
            )
        ),
    new ElementDirectory()
        .setTitle("CSV schema documentation guide")
        .setSlug(ResourcesDirectoryTools.prependRoot("SCHEMA_DOCUMENTATION"))
        .setDescription(
            "General formatting guidelines and data elements guidance  for CSV upload submissions."
        )
        .addElement(
            contentContainer(
                CsvSchemaDocumentation,
                ResourcesDirectoryTools.makeCrumb("CSV schema documentation")
            )
        ),
    new ElementDirectory()
        .setTitle("CSV upload guide")
        .setSlug(ResourcesDirectoryTools.prependRoot("UPLOAD_GUIDE"))
        .setDescription(
            "Instructions for testing facilities and organizations reporting data via comma separated values (CSV)."
        )
        .addElement(
            contentContainer(
                CsvUploadGuideIa,
                ResourcesDirectoryTools.makeCrumb("CSV upload guide")
            )
        ),
    new ElementDirectory()
        .setTitle("CSV download guide")
        .setSlug(ResourcesDirectoryTools.prependRoot("DOWNLOAD_GUIDE"))
        .setDescription(
            "Instructions for downloading data as comma separated values (CSV) for your public health department."
        )
        .addElement(
            contentContainer(
                DataDownloadGuideIa,
                ResourcesDirectoryTools.makeCrumb("CSV download guide")
            )
        ),
    new ElementDirectory()
        .setTitle("ReportStream referral guide")
        .setSlug(ResourcesDirectoryTools.prependRoot("REFERRAL_GUIDE"))
        .setDescription(
            "Instructions and templates for referring reporting entities to use ReportStream in your jurisdiction."
        )
        .addElement(
            contentContainer(
                ReferralGuideMd,
                ResourcesDirectoryTools.makeCrumb("ReportStream referral guide")
            )
        ),
    new ElementDirectory()
        .setTitle("System and settings")
        .setSlug(ResourcesDirectoryTools.prependRoot("SYSTEM"))
        .setDescription(
            "Information about the ReportStream platform, including data storage, configuration, formatting, transport. "
        )
        .addElement(
            contentContainer(
                SystemAndSettingsIa,
                ResourcesDirectoryTools.makeCrumb("System and settings")
            )
        ),
    new ElementDirectory()
        .setTitle("Security practices")
        .setSlug(ResourcesDirectoryTools.prependRoot("SECURITY"))
        .setDescription(
            "Answers to common questions about ReportStream security and data practices."
        )
        .addElement(
            contentContainer(
                SecurityPracticesIa,
                ResourcesDirectoryTools.makeCrumb("Security practices")
            )
        ),
];
