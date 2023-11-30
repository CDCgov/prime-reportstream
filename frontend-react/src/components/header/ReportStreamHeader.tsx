import {
    Button,
    GovBanner,
    Header,
    Menu,
    NavDropDownButton,
    NavMenuButton,
    PrimaryNav,
    Title,
} from "@trussworks/react-uswds";
import classnames from "classnames";
import React, {
    Suspense,
    useCallback,
    useEffect,
    useRef,
    useState,
} from "react";
import { useMatch } from "react-router-dom";

import { USLink, USLinkButton, USSmartLink } from "../USLink";
import SenderModeBanner from "../SenderModeBanner";
import { useSessionContext, RSSessionContext } from "../../contexts/Session";
import { Icon } from "../../shared";
import site from "../../content/site.json";
import Spinner from "../Spinner";
import {
    isOrganizationsMissingTransport,
    useOrganizationSettings__,
} from "../../hooks/UseOrganizationSettings";

import styles from "./ReportStreamHeader.module.scss";

const primaryLinkClasses = (isActive: boolean) => {
    if (isActive) {
        return "text-bold primary-nav-link";
    }

    return "primary-nav-link";
};

export interface DropdownProps extends React.PropsWithChildren {
    onToggle: (name: string) => void;

    menuName: string;
    dropdownList: React.ReactElement[];
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

export interface ReportStreamHeaderProps extends React.PropsWithChildren {
    blueVariant?: boolean;
    isNavHidden?: boolean;
}

interface ReportStreamNavbarProps extends React.PropsWithChildren {
    onToggleMobileNav: () => void;
    isMobileNavOpen: boolean;
    user: RSSessionContext["user"];
    containerRef: React.MutableRefObject<HTMLElement | null>;
}

function ReportStreamNavbar({
    children,
    onToggleMobileNav,
    isMobileNavOpen,
    user,
    containerRef,
}: ReportStreamNavbarProps) {
    const [openMenuItem, setOpenMenuItem] = useState<undefined | string>();
    const { data: organization } = useOrganizationSettings__();
    const isOrgMissingTransport = organization
        ? isOrganizationsMissingTransport(organization.name)
        : false;

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

    const menuItemsReceiver = [
        <div className="primary-nav-link-container" key="dashboard">
            <USSmartLink
                className={primaryLinkClasses(!!useMatch("/data-dashboard/*"))}
                href="/data-dashboard"
            >
                Dashboard
            </USSmartLink>
        </div>,
    ];

    const menuItemsReceiverMissingTransport = [
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
            onToggle={setMenu}
            currentMenuName={openMenuItem}
            key="admin"
        />,
    ];
    const navbarItemBuilder = () => {
        let menuItems = [...menuItemsAbout, ...defaultMenuItems];

        if (
            (user.isUserReceiver ||
                user.isUserTransceiver ||
                user.isUserAdmin) &&
            !isOrgMissingTransport
        ) {
            menuItems = [...menuItems, ...menuItemsReceiver];
        }

        if (
            (user.isUserReceiver ||
                user.isUserTransceiver ||
                user.isUserAdmin) &&
            isOrgMissingTransport
        ) {
            menuItems = [...menuItems, ...menuItemsReceiverMissingTransport];
        }

        if (user.isUserSender || user.isUserTransceiver || user.isUserAdmin) {
            menuItems = [...menuItems, ...menuItemsSender];
        }

        if (user.isAdminStrictCheck) {
            menuItems = [...menuItems, ...menuItemsAdmin];
        }

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

const suspenseFallback = <Spinner size={"fullpage"} />;

export const ReportStreamHeader = ({
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
    const navContainerRef = useRef<HTMLDivElement | null>(null);

    return (
        <>
            <GovBanner aria-label="Official government website" />
            {!isNavHidden && <SenderModeBanner />}
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
                                        {config.CLIENT_ENV}
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
                                containerRef={navContainerRef}
                            >
                                <div className="nav-cta-container">
                                    {user.claims ? (
                                        <>
                                            <span className={styles.UserEmail}>
                                                {user.claims.email ?? "Unknown"}
                                            </span>
                                            {user.isUserAdmin && (
                                                <USLinkButton
                                                    outline
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

                                            <Button
                                                id="logout"
                                                type="button"
                                                onClick={logout}
                                            >
                                                Logout
                                            </Button>
                                        </>
                                    ) : (
                                        <>
                                            <USLinkButton href="/login">
                                                Login
                                            </USLinkButton>
                                            <USLinkButton
                                                href={
                                                    site.forms.connectWithRS.url
                                                }
                                                outline
                                            >
                                                Contact us
                                            </USLinkButton>
                                        </>
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
