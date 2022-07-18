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
import {
    CheckFeatureFlag,
    FeatureFlagName,
} from "../../pages/misc/FeatureFlags";
import { BuiltForYouDropdown } from "../../pages/built-for-you/BuiltForYouIndex";

import { SignInOrUser } from "./SignInOrUser";
import {
    AdminDropdown,
    GettingStartedDropdown,
    HowItWorksDropdown,
} from "./DropdownNav";

const isOktaPreview =
    `${process.env.REACT_APP_OKTA_URL}`.match(/oktapreview.com/) !== null;
const environment = `${process.env.REACT_APP_CLIENT_ENV}`;

export const ReportStreamHeader = () => {
    const { authState } = useOktaAuth();
    const { memberships } = useSessionContext();
    const [expanded, setExpanded] = useState(false);
    let itemsMenu = [<GettingStartedDropdown />, <HowItWorksDropdown />];

    const toggleMobileNav = (): void =>
        setExpanded((prvExpanded) => !prvExpanded);

    if (CheckFeatureFlag(FeatureFlagName.BUILT_FOR_YOU)) {
        itemsMenu.push(<BuiltForYouDropdown />);
    }

    if (CheckFeatureFlag(FeatureFlagName.NEW_IA)) {
        /* TODO: Override itemsMenu with new IA nav */
        itemsMenu.push(
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
        itemsMenu.push(
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
        itemsMenu.push(
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
    }

    if (authState && authState.isAuthenticated && authState.accessToken) {
        /* RECEIVERS ONLY */
        if (
            memberships.state.active?.memberType === MemberType.RECEIVER ||
            memberships.state.active?.memberType === MemberType.PRIME_ADMIN
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
            memberships.state.active?.memberType === MemberType.SENDER ||
            memberships.state.active?.memberType === MemberType.PRIME_ADMIN
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
        if (memberships.state.active?.memberType === MemberType.PRIME_ADMIN) {
            // Validate NavLink
            if (CheckFeatureFlag(FeatureFlagName.VALIDATION_SERVICE)) {
                itemsMenu.push(
                    <NavLink
                        to="/validate"
                        key="validate"
                        data-attribute="hidden"
                        hidden={true}
                        className="usa-nav__link"
                    >
                        <span>Validate</span>
                    </NavLink>
                );
            }

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
                                    {memberships.state.active?.parsedName || ""}
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
