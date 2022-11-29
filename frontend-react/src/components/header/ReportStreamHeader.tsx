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

import { permissionCheck, PERMISSIONS } from "../../utils/PermissionsUtils";
import { ReactComponent as RightLeftArrows } from "../../content/right-left-arrows.svg";
import { useSessionContext } from "../../contexts/SessionContext";
import { MemberType } from "../../hooks/UseOktaMemberships";
import config from "../../config";

import { SignInOrUser } from "./SignInOrUser";
import { AdminDropdown } from "./DropdownNav";

const { IS_PREVIEW, CLIENT_ENV } = config;

const ProductIA = () => (
    <NavLink
        to="/product"
        key="product"
        data-attribute="hidden"
        hidden={true}
        className="usa-nav__link"
    >
        <span>Product</span>
    </NavLink>
);

const ResourcesIA = () => (
    <NavLink
        to="/resources"
        key="resources"
        data-attribute="hidden"
        hidden={true}
        className="usa-nav__link"
    >
        <span>Resources</span>
    </NavLink>
);

const SupportIA = () => (
    <NavLink
        to="/support"
        key="support"
        data-attribute="hidden"
        hidden={true}
        className="usa-nav__link"
    >
        <span>Support</span>
    </NavLink>
);

export const ReportStreamHeader = () => {
    const { authState } = useOktaAuth();
    const { activeMembership, isAdminStrictCheck } = useSessionContext();
    const [expanded, setExpanded] = useState(false);
    let itemsMenu = [<ProductIA />, <ResourcesIA />, <SupportIA />];

    const toggleMobileNav = (): void =>
        setExpanded((prvExpanded) => !prvExpanded);

    if (authState && authState.isAuthenticated && authState.accessToken) {
        /* RECEIVERS ONLY */
        if (
            activeMembership?.memberType === MemberType.RECEIVER ||
            activeMembership?.memberType === MemberType.PRIME_ADMIN
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
            activeMembership?.memberType === MemberType.SENDER ||
            activeMembership?.memberType === MemberType.PRIME_ADMIN
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

        /* ADMIN ONLY (hard check)
          Build a drop-down for file handler links
        */
        if (isAdminStrictCheck) {
            itemsMenu.push(<AdminDropdown />);
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
                                {IS_PREVIEW ? CLIENT_ENV : ""}
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
                    {/* PERMISSIONS REFACTOR
                     This needs to be directly checking the token for admin permissions because
                     an admin with an active membership that is NOT an admin membership type still
                     needs to be able to see and use this */}
                    {permissionCheck(
                        PERMISSIONS.PRIME_ADMIN,
                        authState?.accessToken
                    ) ? (
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
                                    {activeMembership?.parsedName || ""}
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
