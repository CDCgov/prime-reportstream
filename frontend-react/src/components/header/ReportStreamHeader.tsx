import { useOktaAuth } from "@okta/okta-react";
import { useState } from "react";
import { permissionCheck, reportReceiver } from "../../webreceiver-utils";
import { PERMISSIONS } from "../../resources/PermissionsResource";

import { Header, Title, PrimaryNav, Link, NavMenuButton } from "@trussworks/react-uswds";
import { OrganizationDropdown } from './OrgDropdown'
import { SignInOrUser } from "./SignInOrUser";
import { HIWDropdown } from "./HIWDropdown";

export const ReportStreamHeader = () => {
    const { authState } = useOktaAuth();
    const [expanded, setExpanded] = useState(false)
    const toggleMobileNav = (): void => setExpanded((prvExpanded) => !prvExpanded)

    let itemsMenu = [
        <Link href="/about" id="docs" className="usa-nav__link">
            <span>About</span>
        </Link>,
        <HIWDropdown />,
    ];

    if (authState !== null && authState.isAuthenticated) {
        if (reportReceiver(authState)) {
            itemsMenu.splice(0, 0,
                <Link href="/daily-data"
                    key="daily"
                    data-attribute="hidden"
                    hidden={true}
                    className="usa-nav__link">
                    <span>Daily data</span>
                </Link>
            );
        }

        if (permissionCheck(PERMISSIONS.SENDER, authState)) {
            itemsMenu.splice(1, 0,
                <Link href="/upload"
                    key="upload"
                    data-attribute="hidden"
                    hidden={true}
                    className="usa-nav__link">
                    <span>Upload</span>
                </Link>
            );
        }
    }

    return (
        <Header basic={true}>
            <div className="usa-nav-container">
                <div className="usa-navbar margin-right-5">
                    <Title>
                        <a href="/">ReportStream</a>
                    </Title>
                    <NavMenuButton onClick={toggleMobileNav} label="Menu" />
                </div>
                <PrimaryNav
                    items={itemsMenu}
                    onToggleMobileNav={toggleMobileNav}
                    mobileExpanded={expanded}>
                    {
                        authState !== null && authState.isAuthenticated ?
                            <OrganizationDropdown />
                            :
                            null
                    }
                    <SignInOrUser />
                </PrimaryNav>
            </div>
        </Header>
    );
};
