import { Menu, NavDropDownButton } from "@trussworks/react-uswds";
import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";

const HowItWorksDropdown = () => {
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
        <NavLink to="/how-it-works/about">About</NavLink>,
        <NavLink to="/how-it-works/where-were-live">Where we're live</NavLink>,
        <NavLink to="/how-it-works/systems-and-settings">
            System and settings
        </NavLink>,
        <NavLink to="/how-it-works/security-practices">
            Security practices
        </NavLink>,
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

export { HowItWorksDropdown };
