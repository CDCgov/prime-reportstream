import { USNavLink } from "../../components/USLink";

export function primaryLinkClasses(isActive: boolean) {
    if (isActive) {
        return "text-bold primary-nav-link";
    }

    return "primary-nav-link";
}

const defaultMenu: [string, string][] = [
    ["/getting-started", "Getting started"],
    ["/developer-resources", "Developers"],
    ["/managing-your-connection", "Your connection"],
    ["/support", "Support"],
];
export const defaultItems = defaultMenu.map(([url, label]) => (
    <div className="primary-nav-link-container" key={url}>
        <USNavLink className={primaryLinkClasses} href={url}>
            {label}
        </USNavLink>
    </div>
));

const aboutNav: [string, string][] = [
    ["/about", "About Reportstream"],
    ["/about/our-network", "Our network"],
    ["/about/news", "News"],
    ["/about/case-studies", "Case studies"],
    ["/about/security", "Security"],
    ["/about/release-notes", "Release Notes"],
];
export const aboutItems = aboutNav.map(([url, label]) => (
    <USNavLink href={url} key={url}>
        {label}
    </USNavLink>
));

const userTypeNav = [
    ["/data-dashboard", "Dashboard"],
    ["/daily-data", "Daily Data"],
    ["/submissions", "Submissions"],
];

export const [dataDashboardItem, dailyDataItem, submissionsItem] =
    userTypeNav.map(([url, label]) => (
        <USNavLink className={primaryLinkClasses} href={url} key={url}>
            {label}
        </USNavLink>
    ));

const adminNav: [string, string][] = [
    ["/admin/settings", "Organization Settings"],
    ["/admin/features", "Feature Flags"],
    ["/admin/lastmile", "Last Mile Failures"],
    ["/admin/message-tracker", "Message Id Search"],
    ["/admin/send-dash", "Receiver Status Dashboard"],
    ["/admin/value-sets", "Value Sets"],
    ["/file-handler/validate", "Validate"],
];
export const adminItems = adminNav.map(([url, label]) => (
    <USNavLink href={url} key={url}>
        {label}
    </USNavLink>
));
