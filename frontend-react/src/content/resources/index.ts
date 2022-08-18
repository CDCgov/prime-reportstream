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
import { ContentMap } from "../../components/Content/Templates/IACardGridTemplate";

enum ResourcesDirectories {
    ACCOUNT_REGISTRATION = "Account registration guide",
    SCHEMA_DOCUMENTATION = "CSV schema documentation guide",
    UPLOAD_GUIDE = "CSV upload guide",
    DOWNLOAD_GUIDE = "CSV download guide",
    REFERRAL_GUIDE = "ReportStream referral guide",
    PROGRAMMERS_GUIDE = "API Programmer's guide",
    ELR_CHECKLIST = "ELR onboarding checklist",
    SYSTEM = "System and settings",
    SECURITY = "Security practices",
    GETTING_STARTED_PHD = "Getting started: Public health departments",
}
/** Data that drives breadcrumb creation and slug appending
 * @todo: Refactor to make easier for content/design to create */
const slugs: SlugParams[] = [
    {
        key: ResourcesDirectories.ACCOUNT_REGISTRATION,
        slug: "account-registration-guide",
    },
    { key: ResourcesDirectories.SCHEMA_DOCUMENTATION, slug: "csv-schema" },
    { key: ResourcesDirectories.UPLOAD_GUIDE, slug: "csv-upload-guide" },
    { key: ResourcesDirectories.DOWNLOAD_GUIDE, slug: "data-download-guide" },
    { key: ResourcesDirectories.REFERRAL_GUIDE, slug: "referral-guide" },
    { key: ResourcesDirectories.PROGRAMMERS_GUIDE, slug: "programmers-guide" },
    { key: ResourcesDirectories.ELR_CHECKLIST, slug: "elr-checklist" },
    { key: ResourcesDirectories.SYSTEM, slug: "system-and-settings" },
    { key: ResourcesDirectories.SECURITY, slug: "security-practices" },
    {
        key: ResourcesDirectories.GETTING_STARTED_PHD,
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
        .setTitle(ResourcesDirectories.ACCOUNT_REGISTRATION)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(
                ResourcesDirectories.ACCOUNT_REGISTRATION
            )
        )
        .setDescription(
            "Step-by-step instructions for setting up a new user account."
        )
        .addElement(
            contentContainer(
                AccountRegistrationGuideIa,
                ResourcesDirectoryTools.makeCrumb(
                    ResourcesDirectories.ACCOUNT_REGISTRATION
                )
            )
        ),
    new ElementDirectory()
        .setTitle(ResourcesDirectories.GETTING_STARTED_PHD)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(
                ResourcesDirectories.GETTING_STARTED_PHD
            )
        )
        .setDescription(
            "Step-by-step process for connecting your jurisdiction to ReportStream."
        )
        .addElement(
            contentContainer(
                GettingStartedPhd,
                ResourcesDirectoryTools.makeCrumb(
                    ResourcesDirectories.GETTING_STARTED_PHD
                )
            )
        ),
    new ElementDirectory()
        .setTitle(ResourcesDirectories.ELR_CHECKLIST)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(
                ResourcesDirectories.ELR_CHECKLIST
            )
        )
        .setDescription(
            "Checklist of required information for public health departments to set up an ELR connection."
        )
        .addElement(
            contentContainer(
                ELRChecklistIa,
                ResourcesDirectoryTools.makeCrumb(
                    ResourcesDirectories.ELR_CHECKLIST
                )
            )
        ),
    new ElementDirectory()
        .setTitle(ResourcesDirectories.PROGRAMMERS_GUIDE)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(
                ResourcesDirectories.PROGRAMMERS_GUIDE
            )
        )
        .setDescription(
            "Checklist of requirements for  setting up an ELR connection at your public health department."
        )
        .addElement(
            contentContainer(
                ProgrammersGuide,
                ResourcesDirectoryTools.makeCrumb(
                    ResourcesDirectories.PROGRAMMERS_GUIDE
                )
            )
        ),
    new ElementDirectory()
        .setTitle(ResourcesDirectories.SCHEMA_DOCUMENTATION)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(
                ResourcesDirectories.SCHEMA_DOCUMENTATION
            )
        )
        .setDescription(
            "General formatting guidelines and data elements guidance  for CSV upload submissions."
        )
        .addElement(
            contentContainer(
                CsvSchemaDocumentation,
                ResourcesDirectoryTools.makeCrumb(
                    ResourcesDirectories.SCHEMA_DOCUMENTATION
                )
            )
        ),
    new ElementDirectory()
        .setTitle(ResourcesDirectories.UPLOAD_GUIDE)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(
                ResourcesDirectories.UPLOAD_GUIDE
            )
        )
        .setDescription(
            "Instructions for testing facilities and organizations reporting data via comma separated values (CSV)."
        )
        .addElement(
            contentContainer(
                CsvUploadGuideIa,
                ResourcesDirectoryTools.makeCrumb(
                    ResourcesDirectories.UPLOAD_GUIDE
                )
            )
        ),
    new ElementDirectory()
        .setTitle(ResourcesDirectories.DOWNLOAD_GUIDE)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(
                ResourcesDirectories.DOWNLOAD_GUIDE
            )
        )
        .setDescription(
            "Instructions for downloading data as comma separated values (CSV) for your public health department."
        )
        .addElement(
            contentContainer(
                DataDownloadGuideIa,
                ResourcesDirectoryTools.makeCrumb(
                    ResourcesDirectories.DOWNLOAD_GUIDE
                )
            )
        ),
    new ElementDirectory()
        .setTitle(ResourcesDirectories.REFERRAL_GUIDE)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(
                ResourcesDirectories.REFERRAL_GUIDE
            )
        )
        .setDescription(
            "Instructions and templates for referring reporting entities to use ReportStream in your jurisdiction."
        )
        .addElement(
            contentContainer(
                ReferralGuideMd,
                ResourcesDirectoryTools.makeCrumb(
                    ResourcesDirectories.REFERRAL_GUIDE
                )
            )
        ),
    new ElementDirectory()
        .setTitle(ResourcesDirectories.SYSTEM)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(ResourcesDirectories.SYSTEM)
        )
        .setDescription(
            "Information about the ReportStream platform, including data storage, configuration, formatting, transport. "
        )
        .addElement(
            contentContainer(
                SystemAndSettingsIa,
                ResourcesDirectoryTools.makeCrumb(ResourcesDirectories.SYSTEM)
            )
        ),
    new ElementDirectory()
        .setTitle(ResourcesDirectories.SECURITY)
        .setSlug(
            ResourcesDirectoryTools.prependRoot(ResourcesDirectories.SECURITY)
        )
        .setDescription(
            "Answers to common questions about ReportStream security and data practices."
        )
        .addElement(
            contentContainer(
                SecurityPracticesIa,
                ResourcesDirectoryTools.makeCrumb(ResourcesDirectories.SECURITY)
            )
        ),
];

const sampleSectionOne: ResourcesDirectories[] = [
    ResourcesDirectories.DOWNLOAD_GUIDE,
    ResourcesDirectories.UPLOAD_GUIDE,
    ResourcesDirectories.REFERRAL_GUIDE,
];

const sectionOneDirectories = resourcesDirectories.filter((dir) =>
    sampleSectionOne.includes(dir.title as ResourcesDirectories)
);
const theRest = resourcesDirectories.filter(
    (dir) => !sampleSectionOne.includes(dir.title as ResourcesDirectories)
);

export const resourcesContentMap: ContentMap = new Map()
    .set("Guides", sectionOneDirectories)
    .set("The Rest", theRest);
