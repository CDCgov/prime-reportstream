import { useOktaAuth } from "@okta/okta-react";
import { useState } from "react";
import {
    Header,
    Title,
    PrimaryNav,
    NavMenuButton,
} from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";

import { permissionCheck } from "../../webreceiver-utils";
import { PERMISSIONS } from "../../resources/PermissionsResource";

import { OrganizationDropdown } from "./OrgDropdown";
import { SignInOrUser } from "./SignInOrUser";
import { HowItWorksDropdown } from "./HowItWorksDropdown";
import { AdminDropdownNav } from "./AdminDropdownNav";
import { GettingStartedDropdown } from "./GettingStartedDropdown";

export const ReportStreamHeader = () => {
    const { authState } = useOktaAuth();
    const [expanded, setExpanded] = useState(false);
    const toggleMobileNav = (): void =>
        setExpanded((prvExpanded) => !prvExpanded);
    let itemsMenu = [<GettingStartedDropdown />, <HowItWorksDropdown />];

    if (authState !== null && authState.isAuthenticated) {
        if (
            permissionCheck(PERMISSIONS.RECEIVER, authState) ||
            permissionCheck(PERMISSIONS.PRIME_ADMIN, authState)
        ) {
            itemsMenu.push(
                <NavLink
                    to="/daily-data"
                    key="daily"
                    data-attribute="hidden"
                    hidden={true}
                    className="usa-nav__link"
                >
                    <span>Daily data</span>
                </NavLink>
            );
        }

        if (
            permissionCheck(PERMISSIONS.SENDER, authState) ||
            permissionCheck(PERMISSIONS.PRIME_ADMIN, authState)
        ) {
            itemsMenu.push(
                <NavLink
                    to="/upload"
                    key="upload"
                    data-attribute="hidden"
                    hidden={true}
                    className="usa-nav__link"
                >
                    <span>Upload</span>
                </NavLink>
            );
        }

        if (
            // permissionCheck(PERMISSIONS.SENDER, authState) ||
            permissionCheck(PERMISSIONS.PRIME_ADMIN, authState)
        ) {
            itemsMenu.push(
                <NavLink
                    to="/submissions"
                    key="submissions"
                    data-attribute="hidden"
                    hidden={true}
                    className="usa-nav__link"
                >
                    <span>Submissions</span>
                </NavLink>
            );
        }

        if (permissionCheck(PERMISSIONS.PRIME_ADMIN, authState)) {
            itemsMenu.push(<AdminDropdownNav />);
        }
    }

    return (
        <Header basic={true}>
            <div className="usa-nav-container">
                <div className="usa-navbar">
                    <div className="usa-logo" id="basic-logo">
                        <Title>
                            <em className="usa-logo__text font-sans-md">
                                <NavLink to="/" title="Home" aria-label="Home">
                                    ReportStream
                                </NavLink>
                            </em>
                        </Title>
                    </div>
                    <NavMenuButton onClick={toggleMobileNav} label="Menu" />
                </div>
                <PrimaryNav
                    items={itemsMenu}
                    onToggleMobileNav={toggleMobileNav}
                    mobileExpanded={expanded}
                >
                    {authState?.accessToken?.claims?.organization.includes(
                        PERMISSIONS.PRIME_ADMIN
                    ) ? (
                        <OrganizationDropdown />
                    ) : null}
                    <SignInOrUser />
                </PrimaryNav>
            </div>
        </Header>
    );
};
