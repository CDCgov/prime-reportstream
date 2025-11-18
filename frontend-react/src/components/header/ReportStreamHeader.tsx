import {
    GovBanner,
    Header,
    Menu,
    NavMenuButton,
    PrimaryNav,
    Title,
} from "@trussworks/react-uswds";
import classnames from "classnames";
import {
    PropsWithChildren,
    ReactElement,
    Suspense,
    useCallback,
    useEffect,
    useState,
} from "react";
import { useMatch } from "react-router-dom";

import styles from "./ReportStreamHeader.module.scss";
import site from "../../content/site.json";
import { RSSessionContext } from "../../contexts/Session/SessionProvider";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { Icon } from "../../shared";
import { MembershipSettings } from "../../utils/OrganizationUtils";
import SenderModeBanner from "../SenderModeBanner";
import SunsetNoticeBanner from "../SunsetNoticeBanner";
import Spinner from "../Spinner";
import { USLink, USLinkButton, USSmartLink } from "../USLink";

const primaryLinkClasses = (isActive: boolean) => {
    if (isActive) {
        return "text-bold primary-nav-link";
    }

    return "primary-nav-link";
};

export interface DropdownProps extends PropsWithChildren {
    activeDropdown: string | null;
    dropdownList: ReactElement[];
    menuName: string;
    setActiveDropdown: (menuName: string | null) => void;
}

function Dropdown({
    activeDropdown,
    dropdownList,
    menuName,
    setActiveDropdown,
}: DropdownProps) {
    const isCurrentDropdown = activeDropdown === menuName;
    const handleToggle = (e: React.MouseEvent<HTMLButtonElement>) => {
        e.stopPropagation();
        if (isCurrentDropdown) {
            setActiveDropdown(null);
        } else {
            setActiveDropdown(menuName);
        }
    };
    const classes = classnames("usa-accordion__button", "usa-nav__link", {
        "usa-current": isCurrentDropdown,
    });

    return (
        <>
            <button
                data-testid="navDropDownButton"
                className={classes}
                aria-expanded={isCurrentDropdown}
                aria-controls={menuName.toLowerCase()}
                type="button"
                onClick={(e) => {
                    handleToggle(e);
                }}
            >
                <span>{menuName}</span>
            </button>
            <Menu
                items={dropdownList}
                isOpen={isCurrentDropdown}
                id={`${menuName}Dropdown`}
            />
        </>
    );
}

export interface ReportStreamHeaderProps extends PropsWithChildren {
    blueVariant?: boolean;
    isNavHidden?: boolean;
}

interface ReportStreamNavbarProps extends PropsWithChildren {
    activeDropdown: string | null;
    activeMembership: MembershipSettings | null | undefined;
    isMobileNavOpen?: boolean;
    logout: () => void;
    onToggleMobileNav?: () => void;
    setActiveDropdown: (menuName: string | null) => void;
    user: RSSessionContext["user"];
}

function ReportStreamAuthNavbar({
    activeDropdown,
    activeMembership,
    children,
    logout,
    setActiveDropdown,
    user,
}: ReportStreamNavbarProps) {
    const defaultMenuItems = [
        <div className="primary-nav-link-container" key="user-email">
            {user.claims && (
                <span className={styles.UserEmail}>
                    {user.claims.email ?? "Unknown"}
                </span>
            )}
        </div>,
    ];

    const menuOrganization = [
        <div
            className="primary-nav-link-container org-settings"
            key="organization"
        >
            <USLinkButton
                unstyled
                data-testid="org-settings"
                href="/admin/settings"
            >
                {activeMembership?.parsedName ?? " "}
                <Icon name="Loop" className="text-tbottom" />
            </USLinkButton>
        </div>,
    ];

    const menuItemsReceiver = [
        <div className="primary-nav-link-container" key="daily">
            <USLinkButton
                className={primaryLinkClasses(!!useMatch("/daily-data/*"))}
                unstyled
                data-testid="daily-data"
                href="/daily-data"
            >
                Daily Data
            </USLinkButton>
        </div>,
    ];

    const menuItemsSender = [
        <div className="primary-nav-link-container" key="submissions">
            <USLinkButton
                className={primaryLinkClasses(!!useMatch("/submissions/*"))}
                unstyled
                data-testid="submissions"
                href="/submissions"
            >
                Submission History
            </USLinkButton>
        </div>,
    ];

    const menuItemsAuth = [
        <Dropdown
            menuName="Admin tools"
            dropdownList={[
                <USSmartLink href="/admin/settings" key="settings">
                    Organization Settings
                </USSmartLink>,
                <USSmartLink href="/admin/features" key="features">
                    Feature Flags
                </USSmartLink>,
                <USSmartLink href="/admin/lastmile" key="lastmile">
                    Last Mile Failures
                </USSmartLink>,
                <USSmartLink
                    href="/admin/message-tracker"
                    key="message-tracker"
                >
                    Message Id Search
                </USSmartLink>,
                <USSmartLink href="/admin/send-dash" key="send-dash">
                    Receiver Status Dashboard
                </USSmartLink>,
                <USSmartLink href="/admin/value-sets" key="value-sets">
                    Value Sets
                </USSmartLink>,
            ]}
            activeDropdown={activeDropdown}
            setActiveDropdown={setActiveDropdown}
            key="admin"
        />,
    ];

    const menuLogOut = [
        <div className="primary-nav-link-container" key="logout">
            <USLinkButton
                id="logout"
                data-testid="logout"
                unstyled
                onClick={logout}
            >
                Log out
            </USLinkButton>
        </div>,
    ];
    const navbarItemBuilder = () => {
        let menuItems = [...defaultMenuItems];

        if (user.isUserSender || user.isUserTransceiver || user.isUserAdmin) {
            menuItems = [...menuItems, ...menuItemsSender];
        }

        if (user.isUserReceiver || user.isUserTransceiver || user.isUserAdmin) {
            menuItems = [...menuItems, ...menuItemsReceiver];
        }

        if (user.isAdminStrictCheck) {
            menuItems = [
                ...defaultMenuItems,
                ...menuOrganization,
                ...menuItemsSender,
                ...menuItemsReceiver,
                ...menuItemsAuth,
            ];
        }
        menuItems = [...menuItems, ...menuLogOut];

        return menuItems;
    };

    return (
        <>
            <div className={`usa-overlay`}></div>
            <PrimaryNav items={navbarItemBuilder()}>{children}</PrimaryNav>
        </>
    );
}

function ReportStreamNavbar({
    activeDropdown,
    children,
    isMobileNavOpen,
    onToggleMobileNav,
    setActiveDropdown,
    user,
}: ReportStreamNavbarProps) {
    const defaultMenuItems = [
        <div className="primary-nav-link-container" key="developer-resources">
            <USSmartLink
                className={primaryLinkClasses(
                    !!useMatch("/developer-resources/*"),
                )}
                href="/developer-resources"
            >
                Developers
            </USSmartLink>
        </div>,
        <div
            className="primary-nav-link-container"
            key="managing-your-connection"
        >
            <USSmartLink
                className={primaryLinkClasses(
                    !!useMatch("/managing-your-connection/*"),
                )}
                href="/managing-your-connection"
            >
                Your connection
            </USSmartLink>
        </div>,
        <div className="primary-nav-link-container" key="support">
            <USSmartLink
                className={primaryLinkClasses(!!useMatch("/support/*"))}
                href="/support"
            >
                Support
            </USSmartLink>
        </div>,
    ];

    const menuItemsAbout = [
        <Dropdown
            menuName="About"
            dropdownList={[
                <USSmartLink href="/about" key="our-network">
                    About ReportStream
                </USSmartLink>,
                <USSmartLink href="/about/our-network" key="our-network">
                    Our network
                </USSmartLink>,
                <USSmartLink href="/about/roadmap" key="roadmap">
                    Product roadmap
                </USSmartLink>,
                <USSmartLink href="/about/news" key="news">
                    News
                </USSmartLink>,
                <USSmartLink href="/about/case-studies" key="case-studies">
                    Case studies
                </USSmartLink>,
                <USSmartLink href="/about/security" key="security">
                    Security
                </USSmartLink>,
                <USSmartLink href="/about/release-notes" key="release-notes">
                    Release notes
                </USSmartLink>,
            ]}
            activeDropdown={activeDropdown}
            setActiveDropdown={setActiveDropdown}
            key="about"
        />,
    ];

    const menuItemsGettingStarted = [
        <Dropdown
            menuName="Getting started"
            dropdownList={[
                <USSmartLink
                    href="/getting-started/sending-data"
                    key="sending-data"
                >
                    Sending data
                </USSmartLink>,
                <USSmartLink
                    href="/getting-started/receiving-data"
                    key="receiving-data"
                >
                    Receiving data
                </USSmartLink>,
            ]}
            activeDropdown={activeDropdown}
            setActiveDropdown={setActiveDropdown}
            key="getting-started"
        />,
    ];

    const menuItemsReceiver = [
        <div className="primary-nav-link-container" key="daily">
            <USSmartLink
                className={primaryLinkClasses(!!useMatch("/daily-data/*"))}
                href="/daily-data"
            >
                Daily Data
            </USSmartLink>
        </div>,
    ];

    const menuItemsSender = [
        <div className="primary-nav-link-container" key="submissions">
            <USSmartLink
                className={primaryLinkClasses(!!useMatch("/submissions/*"))}
                href="/submissions"
            >
                Submissions
            </USSmartLink>
        </div>,
    ];

    const menuItemsAdmin = [
        <Dropdown
            menuName="Admin"
            dropdownList={[
                <USSmartLink href="/admin/settings" key="settings">
                    Organization Settings
                </USSmartLink>,
                <USSmartLink href="/admin/features" key="features">
                    Feature Flags
                </USSmartLink>,
                <USSmartLink href="/admin/lastmile" key="lastmile">
                    Last Mile Failures
                </USSmartLink>,
                <USSmartLink
                    href="/admin/message-tracker"
                    key="message-tracker"
                >
                    Message Id Search
                </USSmartLink>,
                <USSmartLink href="/admin/send-dash" key="send-dash">
                    Receiver Status Dashboard
                </USSmartLink>,
                <USSmartLink href="/admin/value-sets" key="value-sets">
                    Value Sets
                </USSmartLink>,
                <USSmartLink href="/file-handler/validate" key="validate">
                    Validate
                </USSmartLink>,
            ]}
            activeDropdown={activeDropdown}
            setActiveDropdown={setActiveDropdown}
            key="admin"
        />,
    ];

    let menuItems = [
        ...menuItemsAbout,
        ...menuItemsGettingStarted,
        ...defaultMenuItems,
    ];

    if (isMobileNavOpen) {
        if (user.isUserSender || user.isUserTransceiver || user.isUserAdmin) {
            menuItems = [...menuItems, ...menuItemsSender];
        }

        if (user.isUserReceiver || user.isUserTransceiver || user.isUserAdmin) {
            menuItems = [...menuItems, ...menuItemsReceiver];
        }

        if (user.isAdminStrictCheck) {
            menuItems = [...menuItems, ...menuItemsAdmin];
        }
    }

    const navbarItemBuilder = () => {
        return menuItems;
    };

    return (
        <>
            <button
                onClick={onToggleMobileNav}
                className={`usa-overlay ${isMobileNavOpen ? "is-visible" : ""}`}
            ></button>
            <PrimaryNav
                items={navbarItemBuilder()}
                mobileExpanded={isMobileNavOpen}
                onToggleMobileNav={onToggleMobileNav}
            >
                {children}
            </PrimaryNav>
        </>
    );
}

const suspenseFallback = <Spinner size={"fullpage"} />;

const ReportStreamHeader = ({
    blueVariant,
    children,
    isNavHidden,
}: ReportStreamHeaderProps) => {
    const [activeDropdown, setActiveDropdown] = useState<string | null>(null);
    const { config, user, activeMembership, logout } = useSessionContext();
    const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
    const toggleMobileNav = useCallback(
        () => setIsMobileNavOpen((v) => !v),
        [],
    );
    const handleClickOutside = () => {
        setActiveDropdown(null);
    };

    useEffect(() => {
        document.addEventListener("click", handleClickOutside);
        return () => {
            document.removeEventListener("click", handleClickOutside);
        };
    }, []);

    return (
        <>
            <GovBanner aria-label="Official government website" />
            <SunsetNoticeBanner />
            {!isNavHidden && <SenderModeBanner />}
            {!isNavHidden && (activeMembership ?? user.claims) && (
                <Header
                    data-testid="auth-header"
                    basic={true}
                    className={classnames(styles.AuthNavbar)}
                >
                    <div className="usa-nav-container">
                        <Suspense fallback={suspenseFallback}>
                            <ReportStreamAuthNavbar
                                isMobileNavOpen={isMobileNavOpen}
                                onToggleMobileNav={toggleMobileNav}
                                user={user}
                                activeMembership={activeMembership}
                                logout={logout}
                                activeDropdown={activeDropdown}
                                setActiveDropdown={setActiveDropdown}
                            ></ReportStreamAuthNavbar>
                        </Suspense>
                    </div>
                </Header>
            )}
            <Header
                basic={true}
                className={classnames(styles.Navbar, {
                    [styles.NavbarBlueVariant]: blueVariant,
                    [styles.NavbarDefault]: !blueVariant,
                })}
            >
                <div className="usa-nav-container">
                    <div className="usa-navbar">
                        <Title>
                            <USLink href="/" title="Home" aria-label="Home">
                                ReportStream
                                {config.IS_PREVIEW && (
                                    <span className={styles.ClientEnv}>
                                        {config.MODE}
                                    </span>
                                )}
                            </USLink>
                        </Title>
                        <NavMenuButton onClick={toggleMobileNav} label="Menu" />
                    </div>

                    {!isNavHidden && (
                        <Suspense fallback={suspenseFallback}>
                            <ReportStreamNavbar
                                isMobileNavOpen={isMobileNavOpen}
                                onToggleMobileNav={toggleMobileNav}
                                activeMembership={activeMembership}
                                logout={logout}
                                activeDropdown={activeDropdown}
                                setActiveDropdown={setActiveDropdown}
                                user={user}
                            >
                                <div className="nav-cta-container">
                                    {user.claims && isMobileNavOpen && (
                                        <>
                                            <p className="nav-cta-username">
                                                {user.claims.email ?? "Unknown"}
                                            </p>
                                            {user.isUserAdmin && (
                                                <USLinkButton
                                                    outline
                                                    data-testid="org-settings"
                                                    href="/admin/settings"
                                                >
                                                    {activeMembership?.parsedName ??
                                                        " "}
                                                    <Icon
                                                        name="Loop"
                                                        className="text-tbottom"
                                                    />
                                                </USLinkButton>
                                            )}
                                            <USLinkButton
                                                id="logout"
                                                type="button"
                                                onClick={logout}
                                            >
                                                Logout
                                            </USLinkButton>
                                        </>
                                    )}
                                    {!user.claims && (
                                        <USLinkButton outline href="/login">
                                            Login
                                        </USLinkButton>
                                    )}
                                    {!isMobileNavOpen && (
                                        <USLinkButton
                                            href={site.forms.connectWithRS.url}
                                        >
                                            Contact us
                                        </USLinkButton>
                                    )}
                                </div>
                            </ReportStreamNavbar>
                        </Suspense>
                    )}
                </div>
                {children}
            </Header>
        </>
    );
};

export default ReportStreamHeader;
