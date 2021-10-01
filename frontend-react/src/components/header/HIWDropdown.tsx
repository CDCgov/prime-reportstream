import { Link, Menu, NavDropDownButton } from "@trussworks/react-uswds";
import { useEffect, useState } from "react";

const HIWDropdown = () => {
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
    )
}

export { HIWDropdown }