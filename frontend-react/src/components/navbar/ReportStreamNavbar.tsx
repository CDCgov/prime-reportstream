import {
    Button,
    Header,
    Menu,
    NavDropDownButton,
    NavMenuButton,
    PrimaryNav,
    Title,
} from "@trussworks/react-uswds";
import classnames from "classnames";
import { useState } from "react";

import styles from "./ReportStreamNavbar.module.scss";

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
                        <Title>ReportStream</Title>
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
                            <Button outline type="button">
                                Login
                            </Button>
                            <Button type="button">Connect now</Button>
                        </div>
                    </PrimaryNav>
                </div>
            </Header>
        </>
    );
};
