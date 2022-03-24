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

import { permissionCheck } from "../../webreceiver-utils";
import { PERMISSIONS } from "../../resources/PermissionsResource";
import { getStoredOrg } from "../GlobalContextProvider";
import { ReactComponent as RightLeftArrows } from "../../content/right-left-arrows.svg";

import { SignInOrUser } from "./SignInOrUser";
import { HowItWorksDropdown } from "./HowItWorksDropdown";
import { AdminDropdownNav } from "./AdminDropdownNav";
import { GettingStartedDropdown } from "./GettingStartedDropdown";

export const ReportStreamHeader = () => {
    const { authState } = useOktaAuth();
    const [expanded, setExpanded] = useState(false);
    const organization = getStoredOrg();
    const toggleMobileNav = (): void =>
        setExpanded((prvExpanded) => !prvExpanded);
    let itemsMenu = [<GettingStartedDropdown />, <HowItWorksDropdown />];
    const isOktaPreview =
        `${process.env.REACT_APP_OKTA_URL}`.match(/oktapreview.com/) !== null;
    const environment = `${process.env.REACT_APP_CLIENT_ENV}`;

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
                            <span className="rs-oktapreview-watermark">
                                {isOktaPreview ? environment : ""}
                            </span>
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
                                <span className="usa-breadcrumb padding-left-2 text-semibold text-no-wrap">
                                    {organization}
                                    <RightLeftArrows
                                        aria-hidden="true"
                                        role="img"
                                        className="rs-fa-right-left-icon padding-x-1 padding-top-1 text-primary-vivid"
                                        width={"3em"}
                                        height={"2em"}
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
