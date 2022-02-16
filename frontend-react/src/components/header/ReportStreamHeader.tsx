import { useOktaAuth } from "@okta/okta-react";
import React, { useState } from "react";
import {
    Header,
    NavMenuButton,
    PrimaryNav,
    Title,
} from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";
import { NetworkErrorBoundary } from "rest-hooks";
import { library } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faRightLeft } from "@fortawesome/free-solid-svg-icons";

import { permissionCheck } from "../../webreceiver-utils";
import { PERMISSIONS } from "../../resources/PermissionsResource";
import { getStoredOrg } from "../GlobalContextProvider";

import { SignInOrUser } from "./SignInOrUser";
import { HowItWorksDropdown } from "./HowItWorksDropdown";
import { AdminDropdownNav } from "./AdminDropdownNav";
import { GettingStartedDropdown } from "./GettingStartedDropdown";

library.add(faRightLeft);

export const ReportStreamHeader = () => {
    const { authState } = useOktaAuth();
    const [expanded, setExpanded] = useState(false);
    const organization = getStoredOrg();
    const toggleMobileNav = (): void =>
        setExpanded((prvExpanded) => !prvExpanded);
    let itemsMenu = [<GettingStartedDropdown />, <HowItWorksDropdown />];

    if (authState !== null && authState.isAuthenticated) {
        /* RECEIVERS ONLY */
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

        /* SENDERS ONLY */
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
                </NavLink>,
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

        /* ADMIN ONLY */
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
                    {permissionCheck(PERMISSIONS.PRIME_ADMIN, authState) ? (
                        <NetworkErrorBoundary
                            fallbackComponent={() => (
                                <select>
                                    <option>Network error</option>
                                </select>
                            )}
                        >
                            <NavLink
                                to={`/admin/settings`}
                                className="usa-button usa-button--outline usa-button--small padding-1"
                            >
                                <span className="usa-breadcrumb padding-left-2 text-semibold">
                                    {organization}
                                    <FontAwesomeIcon
                                        className="padding-x-1 padding-top-0"
                                        icon="right-left"
                                        size="sm"
                                    />
                                </span>
                            </NavLink>
                        </NetworkErrorBoundary>
                    ) : null}
                    <SignInOrUser />
                </PrimaryNav>
            </div>
        </Header>
    );
};
