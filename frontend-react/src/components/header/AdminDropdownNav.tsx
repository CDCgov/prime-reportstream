import { Menu, NavDropDownButton } from "@trussworks/react-uswds";
import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";

export const AdminDropdownNav = () => {
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

    const adminMenuItems = [
        <NavLink to="/admin/settings">Organization Settings</NavLink>,
        <NavLink to="/features">Feature Flags</NavLink>,
    ];

    return (
        <>
            <NavDropDownButton
                menuId="adminDropdown"
                onToggle={(): void => {
                    setIsOpen(!isOpen);
                }}
                isOpen={isOpen}
                label="Admin"
                isCurrent={isOpen}
            />
            <Menu
                items={adminMenuItems}
                isOpen={isOpen}
                id="adminDropdown"
                onClick={(): void => setIsOpen(false)}
            />
        </>
    );
};
