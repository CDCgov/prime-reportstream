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
import {
    Suspense,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import { useMatch } from "react-router-dom";

import { USLink, USLinkButton, USSmartLink } from "../USLink";
import config from "../../config";
import SenderModeBanner from "../SenderModeBanner";
import { useSessionContext } from "../../contexts/SessionContext";
import { Icon } from "../../shared";
import site from "../../content/site.json";
import {
    ReceiverOrganizationsMissingTransport,
    useOrganizationSettings,
} from "../../hooks/UseOrganizationSettings";
import Spinner from "../Spinner";

import styles from "./ReportStreamHeader.module.scss";

const { IS_PREVIEW, CLIENT_ENV } = config;

const primaryLinkClasses = (isActive: boolean) => {
    if (isActive) {
        return "text-bold primary-nav-link";
    }

    return "primary-nav-link";
};

const isOrganizationsMissingTransport = (orgName: string): boolean => {
    return ReceiverOrganizationsMissingTransport.indexOf(orgName) > -1;
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
    isSimple?: boolean;
}

interface ReportStreamNavbarProps extends React.PropsWithChildren {
    onMobileMenuOpen: () => void;
}

function ReportStreamNavbar({ onMobileMenuOpen }: ReportStreamNavbarProps) {
    const {
        activeMembership,
        isAdminStrictCheck,
        isUserAdmin,
        isUserReceiver,
        isUserSender,
        user,
        logout,
    } = useSessionContext();
    const navContainerRef = useRef<HTMLDivElement | null>(null);
    const [openMenuItem, setOpenMenuItem] = useState<undefined | string>();
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const toggleMobileMenu = () => {
        if (mobileMenuOpen) {
            setMobileMenuOpen(false);
        } else {
            setMobileMenuOpen(true);
        }
    };
    const setMenu = useCallback((menuName?: string) => {
        setOpenMenuItem((curr) => {
            if (curr === menuName) {
                return undefined;
            } else {
                return menuName;
            }
        });
    }, []);
    const { data: organization } = useOrganizationSettings();
    const orgMissingTransport = organization?.name
        ? isOrganizationsMissingTransport(organization?.name)
        : false;

    // handle if we need to close dropdown due to outside clicks
    useEffect(() => {
        function globalClickHandler(ev: MouseEvent) {
            let buttonEle,
                maybeNavContainerEle =
                    ev.target instanceof HTMLElement ? ev.target : undefined;

            // if target is valid, loop through parents to store info for later
            while (
                maybeNavContainerEle !== navContainerRef.current &&
                maybeNavContainerEle != null
            ) {
                if (
                    maybeNavContainerEle.classList.contains(
                        "usa-accordion__button",
                    ) &&
                    maybeNavContainerEle.classList.contains("usa-nav__link")
                )
                    buttonEle = maybeNavContainerEle;
                maybeNavContainerEle =
                    maybeNavContainerEle.parentElement ?? undefined;
            }

            // if the click was outside the nav container or not on a button within,
            // clear current dropdown.
            if (maybeNavContainerEle == null || buttonEle == null) {
                setMenu();
            }
        }
        window.addEventListener("click", globalClickHandler);

        return () => window.removeEventListener("click", globalClickHandler);
    }, [setMenu]);

    useEffect(() => {
        if (mobileMenuOpen) onMobileMenuOpen?.();
    }, [mobileMenuOpen, onMobileMenuOpen]);

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

        if ((isUserReceiver || isUserAdmin) && !orgMissingTransport) {
            menuItems = [...menuItems, ...menuItemsReceiver];
        }

        if ((isUserReceiver || isUserAdmin) && orgMissingTransport) {
            menuItems = [...menuItems, ...menuItemsReceiverMissingTransport];
        }

        if (isUserSender || isUserAdmin) {
            menuItems = [...menuItems, ...menuItemsSender];
        }

        if (isAdminStrictCheck) {
            menuItems = [...menuItems, ...menuItemsAdmin];
        }

        return menuItems;
    };

    return (
        <div className="usa-nav-container" ref={navContainerRef}>
            <div className="usa-navbar">
                <Title>
                    <USLink href="/" title="Home" aria-label="Home">
                        ReportStream
                        {IS_PREVIEW && (
                            <span className={styles.ClientEnv}>
                                {CLIENT_ENV}
                            </span>
                        )}
                    </USLink>
                </Title>
                <NavMenuButton onClick={toggleMobileMenu} label="Menu" />
            </div>
            <PrimaryNav
                items={navbarItemBuilder()}
                mobileExpanded={mobileMenuOpen}
                onToggleMobileNav={toggleMobileMenu}
            >
                <div className="nav-cta-container">
                    {user ? (
                        <>
                            <span className={styles.UserEmail}>
                                {user?.email ?? "Unknown"}
                            </span>
                            {isUserAdmin && (
                                <USLinkButton outline href="/admin/settings">
                                    {activeMembership?.parsedName ?? ""}{" "}
                                    <Icon
                                        name="Loop"
                                        className="text-tbottom"
                                    />
                                </USLinkButton>
                            )}

                            <Button id="logout" type="button" onClick={logout}>
                                Logout
                            </Button>
                        </>
                    ) : (
                        <>
                            <USLinkButton href="/login">Login</USLinkButton>
                            <USLinkButton
                                href={site.forms.connectWithRS.url}
                                outline
                            >
                                Connect now
                            </USLinkButton>
                        </>
                    )}
                </div>
            </PrimaryNav>
        </div>
    );
}

export const ReportStreamHeader = ({
    blueVariant,
    children,
    isSimple = false,
}: ReportStreamHeaderProps) => {
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const suspenseFallback = useMemo(() => <Spinner size={"fullpage"} />, []);
    return (
        <>
            <GovBanner aria-label="Official government website" />
            {!isSimple && <SenderModeBanner />}
            <div
                className={`usa-overlay ${mobileMenuOpen ? "is-visible" : ""}`}
            ></div>
            <Header
                basic={true}
                className={classnames(styles.Navbar, {
                    [styles.NavbarBlueVariant]: blueVariant,
                    [styles.NavbarDefault]: !blueVariant,
                })}
            >
                {!isSimple ? (
                    <Suspense fallback={suspenseFallback}>
                        <ReportStreamNavbar
                            onMobileMenuOpen={() => setMobileMenuOpen(true)}
                        />
                    </Suspense>
                ) : (
                    <div className="usa-nav-container">
                        <div className="usa-navbar">
                            <USLink href="/">
                                <Title>
                                    ReportStream
                                    {IS_PREVIEW && (
                                        <span className={styles.ClientEnv}>
                                            {CLIENT_ENV}
                                        </span>
                                    )}
                                </Title>
                            </USLink>
                        </div>
                    </div>
                )}
                {children}
            </Header>
        </>
    );
};

export default ReportStreamHeader;
