import {
    Header,
    Title,
    PrimaryNav,
    Button,
    Link,
    Dropdown,
    NavDropDownButton,
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

    let setValue = (e: any) => { };

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
        <Link href="/daily">
            <Button type="button" outline>
                Log in
            </Button>
        </Link>
    );
};

export const ReportStreamHeader = () => {
    const { authState } = useOktaAuth();
    const [isOpen, setIsOpen] = useState(false);
    const testMenuItems = [
        <Link href="/how-it-works/getting-started" >
            Getting started
        </Link>,
        <Link href="/how-it-works/elr-checklist" >
            ELR onboarding checklist
        </Link>,
        <Link
            href="/how-it-works/data-download-guide"
        >
            Data download website guide
        </Link>,
        <Link href="/how-it-works/where-were-live" >
            Where we're live
        </Link>,
        <Link
            href="/how-it-works/systems-and-settings"
        >
            System and settings
        </Link>,
        <Link href="/how-it-works/security-practices" >
            Security practices
        </Link>,
    ];

    let itemsMenu = [
        <Link href="/about" id="docs" className="usa-nav__link">
            <span>About</span>
        </Link>,
        <>
            <NavDropDownButton
                menuId="testDropDownOne"
                onToggle={(): void => {
                    setIsOpen(true);
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
        </>,
    ];

    if (authState !== null && authState.isAuthenticated) {
        if (reportReceiver(authState)) {
            itemsMenu.splice(0, 0,
                <Link href="/daily"
                    key="daily"
                    data-attribute="hidden"
                    hidden={true}
                    className="usa-nav__link">
                    <span>Daily data</span>
                </Link>
            );
        }

        if (permissionCheck(PERMISSIONS['sender'], authState)) {
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
                </div>
                <PrimaryNav items={itemsMenu}>
                    <SignInOrUser />
                </PrimaryNav>
            </div>
        </Header>
    );
};
