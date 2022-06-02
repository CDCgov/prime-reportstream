import { Menu, NavDropDownButton } from "@trussworks/react-uswds";
import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";

const SupportDropdown = () => {
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
        <NavLink to="/support/faq">Frequently asked questions</NavLink>,
        <NavLink to="/contact">Contact</NavLink>,
    ];

    return (
        <>
            <NavDropDownButton
                menuId="testDropDownOne"
                onToggle={(): void => {
                    setIsOpen(!isOpen);
                }}
                isOpen={isOpen}
                label="Support"
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

export { SupportDropdown };
