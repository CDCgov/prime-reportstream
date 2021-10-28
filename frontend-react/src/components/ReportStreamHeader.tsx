import {
    Header,
    Title,
    PrimaryNav,
    Button,
    Link,
    Dropdown,
    NavDropDownButton,
    NavMenuButton,
    Menu,
} from "@trussworks/react-uswds";
import { useOktaAuth } from "@okta/okta-react";
import { useEffect, useState } from "react";
import { useResource } from "rest-hooks";

import OrganizationResource from "../resources/OrganizationResource";
import { permissionCheck, reportReceiver } from "../webreceiver-utils";
import { PERMISSIONS } from "../resources/PermissionsResource";

/**
 *
 * @returns OrganizationDropDown
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const OrganizationDropDown = () => {
    let orgs = useResource(OrganizationResource.list(), {
        sortBy: undefined,
    }).sort((a, b) => a.description.localeCompare(b.description));

    let setValue = (e: any) => {};

    return (
        <Dropdown
            id="input-dropdown"
            name="input-dropdown"
            defaultValue="pima-az-phd"
            onChange={(e) => setValue(e.target.value)}
        >
            {orgs.map((org) => (
                <option key={org.name} value={org.name}>
                    {" "}
                    {org.description} ({org.name})
                </option>
            ))}
        </Dropdown>
    );
};

/**
 *
 * @returns SignInOrUser
 */
const SignInOrUser = () => {
    const { oktaAuth, authState } = useOktaAuth();

    const [user, setUser] = useState("");

    useEffect(() => {
        if (authState && authState.isAuthenticated) {
            oktaAuth
                .getUser()
                .then((cl) => setUser(cl.email ? cl.email : "unknown user"));
        }
    });

    return authState && authState.isAuthenticated ? (
        <div className="prime-user-account">
            <span id="emailUser">{user ? user : ""}</span>
            <br />
            <a
                href="/"
                id="logout"
                onClick={() => oktaAuth.signOut()}
                className="usa-link"
            >
                Log out
            </a>
        </div>
    ) : (
        <Link href="/daily-data">
            <Button type="button" outline>
                Log in
            </Button>
        </Link>
    );
};

const DropdownHowItWorks = () => {
    const [isOpen, setIsOpen] = useState(false);

    /* Used since setIsOpen cannot be directly called in useEffect */
    const handleClick = () => setIsOpen(false);
    /* INFO
       This has to be down on "mouseup" not "mousedown" otherwise clicking
       any link in the list will result in the menu closing without registering
       the click on the link; thus, you're not directed to the page desired */
    useEffect(() => {
        document.body.addEventListener("mouseup", handleClick);
        return () => {
            document.body.removeEventListener("mouseup", handleClick);
        };
    }, []);

    const testMenuItems = [
        <Link href="/how-it-works/getting-started">Getting started</Link>,
        <Link href="/how-it-works/elr-checklist">
            ELR onboarding checklist
        </Link>,
        <Link href="/how-it-works/data-download-guide">
            Data download website guide
        </Link>,
        <Link href="/how-it-works/where-were-live">Where we're live</Link>,
        <Link href="/how-it-works/systems-and-settings">
            System and settings
        </Link>,
        <Link href="/how-it-works/security-practices">Security practices</Link>,
    ];

    return (
        <>
            <NavDropDownButton
                menuId="testDropDownOne"
                onToggle={(): void => {
                    setIsOpen(!isOpen);
                }}
                isOpen={isOpen}
                label="How it works"
                isCurrent={isOpen}
            />
            <Menu
                items={testMenuItems}
                isOpen={isOpen}
                id="testDropDownOne"
                onClick={(): void => setIsOpen(false)}
            />
        </>
    );
};

export const ReportStreamHeader = () => {
    const { authState } = useOktaAuth();
    const [expanded, setExpanded] = useState(false);
    const toggleMobileNav = (): void =>
        setExpanded((prvExpanded) => !prvExpanded);

    let itemsMenu = [
        <Link href="/about" id="docs" className="usa-nav__link">
            <span>About</span>
        </Link>,
        <DropdownHowItWorks />,
    ];

    if (authState !== null && authState.isAuthenticated) {
        if (reportReceiver(authState)) {
            itemsMenu.splice(
                0,
                0,
                <Link
                    href="/daily-data"
                    key="daily"
                    data-attribute="hidden"
                    hidden={true}
                    className="usa-nav__link"
                >
                    <span>Daily data</span>
                </Link>
            );
        }

        if (permissionCheck(PERMISSIONS.SENDER, authState)) {
            itemsMenu.splice(
                1,
                0,
                <Link
                    href="/upload"
                    key="upload"
                    data-attribute="hidden"
                    hidden={true}
                    className="usa-nav__link"
                >
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
                    mobileExpanded={expanded}
                >
                    <SignInOrUser />
                </PrimaryNav>
            </div>
        </Header>
    );
};
