import React, { useState } from "react";
import {
    Header,
    NavMenuButton,
    PrimaryNav,
    Title,
} from "@trussworks/react-uswds";
import classnames from "classnames";

import { ReactComponent as RightLeftArrows } from "../../content/right-left-arrows.svg";
import { useSessionContext } from "../../contexts/SessionContext";
import config from "../../config";
import { USLink, USNavLink } from "../USLink";
import { FeatureName } from "../../utils/FeatureName";

import { SignInOrUser } from "./SignInOrUser";
import { AdminDropdown } from "./DropdownNav";

const { IS_PREVIEW, CLIENT_ENV } = config;

const ProductIA = () => (
    <USNavLink href="/product" key="product" data-attribute="hidden">
        <span>Product</span>
    </USNavLink>
);

const ResourcesIA = () => (
    <USNavLink href="/resources" key="resources" data-attribute="hidden">
        <span>Resources</span>
    </USNavLink>
);

const SupportIA = () => (
    <USNavLink href="/support" key="support" data-attribute="hidden">
        <span>Support</span>
    </USNavLink>
);

export interface ReportStreamHeaderProps
    extends React.PropsWithChildren<React.HTMLAttributes<HTMLElement>> {}

export const ReportStreamHeader = ({
    children,
    className,
    ...props
}: ReportStreamHeaderProps) => {
    const {
        activeMembership,
        isAdminStrictCheck,
        isUserAdmin,
        isUserReceiver,
        isUserSender,
    } = useSessionContext();
    const [expanded, setExpanded] = useState(false);
    let itemsMenu = [<ProductIA />, <ResourcesIA />, <SupportIA />];

    const toggleMobileNav = (): void =>
        setExpanded((prvExpanded) => !prvExpanded);

    /* RECEIVERS ONLY */
    if (isUserReceiver || isUserAdmin) {
        itemsMenu.push(
            <USNavLink href="/daily-data" key="daily" data-attribute="hidden">
                <span>{FeatureName.DAILY_DATA}</span>
            </USNavLink>,
        );
    }

    /* SENDERS ONLY */
    if (isUserSender || isUserAdmin) {
        itemsMenu.push(
            <USNavLink href="/upload" key="upload" data-attribute="hidden">
                <span>{FeatureName.UPLOAD}</span>
            </USNavLink>,
            <USNavLink
                href="/submissions"
                key="submissions"
                data-attribute="hidden"
            >
                <span>{FeatureName.SUBMISSIONS}</span>
            </USNavLink>,
        );
    }

    /* ADMIN ONLY (hard check)
          Build a drop-down for file handler links
        */
    if (isAdminStrictCheck) {
        itemsMenu.push(<AdminDropdown />);
    }

    return (
        <Header
            basic={true}
            className={classnames(
                "border-bottom-1px border-base-lighter",
                className,
            )}
            {...props}
        >
            {children}
            <div className="usa-nav-container">
                <div className="usa-navbar">
                    <div className="usa-logo" id="basic-logo">
                        <Title>
                            <em className="usa-logo__text font-sans-md">
                                <USLink
                                    href="/"
                                    title="Home"
                                    aria-label="Home"
                                    className="rs-header-mark"
                                >
                                    ReportStream
                                </USLink>
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
                    {isUserAdmin ? (
                        <USLink
                            href={`/admin/settings`}
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
                        </USLink>
                    ) : null}
                    <SignInOrUser />
                </PrimaryNav>
            </div>
        </Header>
    );
};
