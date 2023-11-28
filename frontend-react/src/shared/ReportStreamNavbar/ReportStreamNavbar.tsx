import {
    Button,
    NavMenuButton,
    PrimaryNav,
    Title,
} from "@trussworks/react-uswds";
import { useState, useCallback, useEffect, useRef, useMemo } from "react";

import { USLink, USLinkButton } from "../../components/USLink";
import { useSessionContext } from "../../contexts/Session";
import {
    useOrganizationSettings__,
    isOrganizationsMissingTransport,
} from "../../hooks/UseOrganizationSettings";
import Icon from "../Icon/Icon";
import { RSUser } from "../../utils/OrganizationUtils";
import styles from "../ReportStreamHeader/ReportStreamHeader.module.scss";

import {
    aboutItems,
    adminItems,
    dailyDataItem,
    dataDashboardItem,
    defaultItems,
    submissionsItem,
} from "./DropdownLinks";
import Dropdown from "./Dropdown";

interface ReportStreamNavbarBaseProps extends React.PropsWithChildren {
    organizationName?: string;
    previewEnv?: string;

    onLogout: () => void;
    onClearImpersonation: () => void;
    user: RSUser;
    contactUsUrl: string;
}

export function ReportStreamNavbarBase({
    children,
    user,
    organizationName,
    onClearImpersonation,
    onLogout,
    previewEnv,
    contactUsUrl,
}: ReportStreamNavbarBaseProps) {
    const [isMobileNavOpen, setIsMobileNavOpen] = useState(false);
    const toggleMobileNav = useCallback(
        () => setIsMobileNavOpen((v) => !v),
        [],
    );
    const containerRef = useRef<HTMLDivElement | null>(null);
    const [openMenuItem, setOpenMenuItem] = useState<undefined | string>();
    const isOrgMissingTransport =
        organizationName && isOrganizationsMissingTransport(organizationName);

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
                if (isMobileNavOpen) toggleMobileNav();
                if (openMenuItem) setMenu();
            }
        }
        window.addEventListener("click", globalClickHandler);

        return () => window.removeEventListener("click", globalClickHandler);
    }, [containerRef, isMobileNavOpen, toggleMobileNav, openMenuItem, setMenu]);

    const navbarItems = useMemo(
        () => [
            <Dropdown
                menuName="About"
                dropdownList={aboutItems}
                currentMenuName={openMenuItem}
                onToggle={setMenu}
                key="about"
            />,
            ...defaultItems,
            user.isReceiver && !isOrgMissingTransport && dataDashboardItem,
            user.isReceiver && isOrgMissingTransport && dailyDataItem,
            user.isSender && submissionsItem,
            user.isAdmin && (
                <Dropdown
                    menuName="Admin"
                    dropdownList={adminItems}
                    onToggle={setMenu}
                    currentMenuName={openMenuItem}
                    key="admin"
                />
            ),
        ],
        [
            isOrgMissingTransport,
            openMenuItem,
            setMenu,
            user.isAdmin,
            user.isReceiver,
            user.isSender,
        ],
    );

    return (
        <div className="usa-nav-container" ref={containerRef}>
            <div
                className={`usa-overlay ${isMobileNavOpen ? "is-visible" : ""}`}
            ></div>
            <div className="usa-navbar">
                <Title>
                    <USLink href="/" title="Home" aria-label="Home">
                        ReportStream
                        {previewEnv && (
                            <span className={styles.ClientEnv}>
                                {previewEnv}
                            </span>
                        )}
                    </USLink>
                </Title>
                <NavMenuButton onClick={toggleMobileNav} label="Menu" />
                <PrimaryNav
                    items={navbarItems}
                    mobileExpanded={isMobileNavOpen}
                    onToggleMobileNav={toggleMobileNav}
                >
                    <div className="nav-cta-container">
                        {!user.isAnonymous ? (
                            <>
                                <span className={styles.UserEmail}>
                                    {user.username ?? "Unknown"}
                                </span>
                                {(user.isImpersonated || user.isAdmin) &&
                                user.isImpersonated ? (
                                    <Button
                                        type="button"
                                        onClick={onClearImpersonation}
                                    >
                                        {user.organization}
                                        <Icon
                                            name="Close"
                                            className="text-tbottom"
                                        />
                                    </Button>
                                ) : (
                                    <USLinkButton
                                        outline
                                        href="/admin/settings"
                                    >
                                        {user.organization}
                                        <Icon
                                            name="Loop"
                                            className="text-tbottom"
                                        />
                                    </USLinkButton>
                                )}

                                <Button
                                    id="logout"
                                    type="button"
                                    onClick={onLogout}
                                >
                                    Logout
                                </Button>
                            </>
                        ) : (
                            <>
                                <USLinkButton href="/login">Login</USLinkButton>
                                <USLinkButton href={contactUsUrl} outline>
                                    Contact us
                                </USLinkButton>
                            </>
                        )}
                    </div>
                </PrimaryNav>
                {children}
            </div>
        </div>
    );
}

export interface ReportStreamNavbarProps extends React.PropsWithChildren {}

export function ReportStreamNavbar(props: ReportStreamNavbarProps) {
    const { config, user, logout, clearImpersonation, site } =
        useSessionContext();
    const previewEnv = config.IS_PREVIEW ? config.CLIENT_ENV : undefined;
    const { data } = useOrganizationSettings__();
    return (
        <ReportStreamNavbarBase
            {...props}
            organizationName={data?.name}
            contactUsUrl={site.forms.contactUs.url}
            onClearImpersonation={clearImpersonation}
            onLogout={logout}
            user={user}
            previewEnv={previewEnv}
        />
    );
}
