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
import { useState } from "react";

import { USLinkButton } from "../USLink";
import config from "../../config";

import styles from "./ReportStreamNavbar.module.scss";
import { DAPHeader } from "../header/DAPHeader";
import SenderModeBanner from "../SenderModeBanner";

const { IS_PREVIEW, CLIENT_ENV, APP_ENV } = config;

export const ReportStreamNavbar = ({
    blueVariant,
}: {
    blueVariant?: boolean;
}) => {
    const [openMenuItem, setOpenMenuItem] = useState<null | string>(null);
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const toggleMobileMenu = () => {
        if (mobileMenuOpen) {
            setMobileMenuOpen(false);
        } else {
            setMobileMenuOpen(true);
        }
    };
    const setMenu = (menuName: string) => {
        if (openMenuItem === menuName) {
            setOpenMenuItem(null);
        } else {
            setOpenMenuItem(menuName);
        }
    };

    const Dropdown = ({
        menuName,
        dropdownList,
    }: {
        menuName: string;
        dropdownList: React.ReactElement[];
    }) => {
        return (
            <>
                <NavDropDownButton
                    menuId={menuName.toLowerCase()}
                    isOpen={openMenuItem === menuName}
                    isCurrent={openMenuItem === menuName}
                    label={menuName}
                    onToggle={() => {
                        setMenu(menuName);
                    }}
                />
                <Menu
                    items={dropdownList}
                    isOpen={openMenuItem === menuName}
                    id={`${menuName}Dropdown`}
                />
            </>
        );
    };
    const menuItems = [
        <div className="primary-nav-link-container">
            <a className="primary-nav-link" href="/product" key="product">
                Product
            </a>
        </div>,
        <div className="primary-nav-link-container">
            <a className="primary-nav-link" href="/resources" key="resources">
                Resources
            </a>
        </div>,
        <div className="primary-nav-link-container">
            <a className="primary-nav-link" href="/support" key="support">
                Support
            </a>
        </div>,
    ];
    return (
        <>
            <DAPHeader env={APP_ENV ? APP_ENV : "production"} />
            <GovBanner aria-label="Official government website" />
            <SenderModeBanner />
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
                <div className="usa-nav-container">
                    <div className="usa-navbar">
                        <Title>
                            ReportStream
                            {IS_PREVIEW && (
                                <span className={styles.ClientEnv}>
                                    {CLIENT_ENV}
                                </span>
                            )}
                        </Title>
                        <NavMenuButton
                            onClick={toggleMobileMenu}
                            label="Menu"
                        />
                    </div>
                    <PrimaryNav
                        items={menuItems}
                        mobileExpanded={mobileMenuOpen}
                        onToggleMobileNav={toggleMobileMenu}
                    >
                        <div className="nav-cta-container">
                            <USLinkButton href="/login">Login</USLinkButton>
                            <USLinkButton
                                href="https://app.smartsheetgov.com/b/form/48f580abb9b440549b1a9cf996ba6957"
                                outline
                            >
                                Connect now
                            </USLinkButton>
                        </div>
                    </PrimaryNav>
                </div>
            </Header>
        </>
    );
};
