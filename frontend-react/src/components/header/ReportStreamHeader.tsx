import {
    GovBanner,
    Header,
    Menu,
    NavDropDownButton,
    NavMenuButton,
    PrimaryNav,
    Title,
} from "@trussworks/react-uswds";
import classnames from "classnames";
import {
    MutableRefObject,
    PropsWithChildren,
    ReactElement,
    Suspense,
    useCallback,
    useEffect,
    useRef,
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
import Spinner from "../Spinner";
import { USLink, USLinkButton, USSmartLink } from "../USLink";

const primaryLinkClasses = (isActive: boolean) => {
    if (isActive) {
        return "text-bold primary-nav-link";
    }

    return "primary-nav-link";
};

export interface DropdownProps extends PropsWithChildren {
    onToggle: (name: string) => void;

    menuName: string;
    dropdownList: ReactElement[];
    currentMenuName?: string;
}

function Dropdown({
    menuName,
    dropdownList,
    currentMenuName,
    onToggle,
}: DropdownProps) {
    return (
        <>
            <NavDropDownButton
                menuId={menuName.toLowerCase()}
                isOpen={currentMenuName === menuName}
                isCurrent={currentMenuName === menuName}
                label={menuName}
                onToggle={() => onToggle(menuName)}
            />
            <Menu
                items={dropdownList}
                isOpen={currentMenuName === menuName}
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
    onToggleMobileNav: () => void;
    isMobileNavOpen: boolean;
    user: RSSessionContext["user"];
    activeMembership: MembershipSettings | null | undefined;
    logout: () => void;
    containerRef: MutableRefObject<HTMLElement | null>;
}

function ReportStreamAuthNavbar({
    children,
    onToggleMobileNav,
    isMobileNavOpen,
    user,
    activeMembership,
    logout,
    containerRef,
}: ReportStreamNavbarProps) {
    const [openAuthMenuItem, setOpenAuthMenuItem] = useState<
        undefined | string
    >();

    const setAuthMenu = useCallback((menuAuthName?: string) => {
        setOpenAuthMenuItem((curr) => {
            if (curr === menuAuthName) {
                return undefined;
            } else {
                return menuAuthName;
            }
        });
    }, []);

    // handle if we need to close menus due to outside clicks
    useEffect(() => {
        function globalClickHandler(ev: MouseEvent) {
            let buttonEle,
                maybeAuthNavContainerEle =
                    ev.target instanceof HTMLElement ? ev.target : undefined;

            // if target is valid, loop through parents to store info for later
            while (
                maybeAuthNavContainerEle !== containerRef.current &&
                maybeAuthNavContainerEle != null
            ) {
                if (
                    maybeAuthNavContainerEle.classList.contains(
                        "usa-menu-btn",
                    ) ||
                    (maybeAuthNavContainerEle.classList.contains(
                        "usa-accordion__button",
                    ) &&
                        maybeAuthNavContainerEle.classList.contains(
                            "usa-nav__link",
                        ))
                )
                    buttonEle = maybeAuthNavContainerEle;
                maybeAuthNavContainerEle =
                    maybeAuthNavContainerEle.parentElement ?? undefined;
            }

            // if the click was outside the nav container or not on a button within,
            // clear current dropdown.
            if (maybeAuthNavContainerEle == null || buttonEle == null) {
                if (isMobileNavOpen) onToggleMobileNav();
                if (openAuthMenuItem) setAuthMenu();
            }
        }
        window.addEventListener("click", globalClickHandler);

        return () => window.removeEventListener("click", globalClickHandler);
    }, [
        containerRef,
        isMobileNavOpen,
        onToggleMobileNav,
        openAuthMenuItem,
        setAuthMenu,
    ]);

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
            <USLinkButton unstyled data-testid="daily-data" href="/daily-data">
                Daily Data
            </USLinkButton>
        </div>,
    ];

    const menuItemsSender = [
        <div className="primary-nav-link-container" key="submissions">
            <USLinkButton
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
                <USSmartLink href="/file-handler/validate" key="validate">
                    Validate
                </USSmartLink>,
            ]}
            onToggle={setAuthMenu}
            currentMenuName={openAuthMenuItem}
            key="admin"
        />,
    ];

    const menuLogOut = [
        <div className="primary-nav-link-container" key="logout">
            {user.claims && (
                <USLinkButton
                    id="logout"
                    data-testid="logout"
                    unstyled
                    onClick={logout}
                >
                    Log out
                </USLinkButton>
            )}
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
            <div
                className={`usa-overlay ${isMobileNavOpen ? "is-visible" : ""}`}
            ></div>
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

function ReportStreamNavbar({
    children,
    onToggleMobileNav,
    isMobileNavOpen,
    containerRef,
}: ReportStreamNavbarProps) {
    const [openMenuItem, setOpenMenuItem] = useState<undefined | string>();

    const setMenu = useCallback((menuName?: string) => {
        setOpenMenuItem((curr) => {
            if (curr === menuName) {
                return undefined;
            } else {
                return menuName;
            }
        });
    }, []);

    // handle if we need to close menus due to outside clicks
    useEffect(() => {
        function globalClickHandler(ev: MouseEvent) {
            let buttonEle,
                maybeNavContainerEle =
                    ev.target instanceof HTMLElement ? ev.target : undefined;

            // if target is valid, loop through parents to store info for later
            while (
                maybeNavContainerEle !== containerRef.current &&
                maybeNavContainerEle != null
            ) {
                if (
                    maybeNavContainerEle.classList.contains("usa-menu-btn") ||
                    (maybeNavContainerEle.classList.contains(
                        "usa-accordion__button",
                    ) &&
                        maybeNavContainerEle.classList.contains(
                            "usa-nav__link",
                        ))
                )
                    buttonEle = maybeNavContainerEle;
                maybeNavContainerEle =
                    maybeNavContainerEle.parentElement ?? undefined;
            }

            // if the click was outside the nav container or not on a button within,
            // clear current dropdown.
            if (maybeNavContainerEle == null || buttonEle == null) {
                if (isMobileNavOpen) onToggleMobileNav();
                if (openMenuItem) setMenu();
            }
        }
        window.addEventListener("click", globalClickHandler);

        return () => window.removeEventListener("click", globalClickHandler);
    }, [
        containerRef,
        isMobileNavOpen,
        onToggleMobileNav,
        openMenuItem,
        setMenu,
    ]);

    const defaultMenuItems = [
        <div className="primary-nav-link-container" key="getting-started">
            <USSmartLink
                className={primaryLinkClasses(!!useMatch("/getting-started/*"))}
                href="/getting-started"
            >
                Getting started
            </USSmartLink>
        </div>,
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
            currentMenuName={openMenuItem}
            onToggle={setMenu}
            key="about"
        />,
    ];
    const navbarItemBuilder = () => {
        return [...menuItemsAbout, ...defaultMenuItems];
    };

    return (
        <>
            <div
                className={`usa-overlay ${isMobileNavOpen ? "is-visible" : ""}`}
            ></div>
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
    const { config, user, activeMembership, logout } = useSessionContext();
    const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
    const toggleMobileNav = useCallback(
        () => setIsMobileNavOpen((v) => !v),
        [],
    );
    const navAuthContainerRef = useRef<HTMLDivElement | null>(null);
    const navContainerRef = useRef<HTMLDivElement | null>(null);

    return (
        <>
            <GovBanner aria-label="Official government website" />
            {!isNavHidden && <SenderModeBanner />}
            {!isNavHidden && user.claims && (
                <Header basic={true} className={classnames(styles.AuthNavbar)}>
                    <div
                        className="usa-nav-container"
                        ref={navAuthContainerRef}
                    >
                        <Suspense fallback={suspenseFallback}>
                            <ReportStreamAuthNavbar
                                isMobileNavOpen={isMobileNavOpen}
                                onToggleMobileNav={toggleMobileNav}
                                user={user}
                                activeMembership={activeMembership}
                                logout={logout}
                                containerRef={navAuthContainerRef}
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
                <div className="usa-nav-container" ref={navContainerRef}>
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
                                user={user}
                                activeMembership={activeMembership}
                                logout={logout}
                                containerRef={navContainerRef}
                            >
                                <div className="nav-cta-container">
                                    {!user.claims && (
                                        <USLinkButton outline href="/login">
                                            Login
                                        </USLinkButton>
                                    )}
                                    <USLinkButton
                                        href={site.forms.connectWithRS.url}
                                    >
                                        Contact us
                                    </USLinkButton>
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
