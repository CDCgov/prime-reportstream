import React, { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import { Menu, NavDropDownButton } from "@trussworks/react-uswds";

import { MarkdownDirectory } from "../Content/MarkdownDirectory";
import { useFeatureFlags } from "../../contexts/FeatureFlagContext";
import { FeatureFlagName } from "../../pages/misc/FeatureFlags";

export interface NonStaticOption {
    title: string;
    root: string;
    slug: string;
}

interface DropdownNavProps {
    label: string;
    root: string;
    directories: MarkdownDirectory[] | NonStaticOption[];
}

export const makeNonStaticOption = (
    title: string,
    slug: string,
    root: string
): NonStaticOption => {
    return { title, slug, root };
};

export const DropdownNav = ({ label, root, directories }: DropdownNavProps) => {
    const [isOpen, setIsOpen] = useState(false);
    /* Used since setIsOpen cannot be directly called in useEffect */
    const handleClick = () => setIsOpen(false);
    /* This has to be down on "mouseup" not "mousedown" otherwise clicking
       any link in the list will result in the menu closing without registering
       the click on the link; thus, you're not directed to the page desired */
    useEffect(() => {
        document.body.addEventListener("mouseup", handleClick);
        return () => {
            document.body.removeEventListener("mouseup", handleClick);
        };
    }, []);
    const navMenu = directories.map((dir) => (
        <NavLink to={`${dir.root}/${dir.slug}`}>{dir.title}</NavLink>
    ));
    return (
        <>
            <NavDropDownButton
                menuId={root}
                onToggle={(): void => {
                    setIsOpen(!isOpen);
                }}
                isOpen={isOpen}
                label={label}
                isCurrent={isOpen}
            />
            <Menu
                items={navMenu}
                isOpen={isOpen}
                id={root}
                onClick={(): void => setIsOpen(false)}
            />
        </>
    );
};

export const AdminDropdown = () => {
    const { checkFlag } = useFeatureFlags();

    const pages = [
        makeNonStaticOption("Organization Settings", "settings", "/admin"),
        makeNonStaticOption("Feature Flags", "features", "/admin"),
        makeNonStaticOption("Last Mile Failures", "lastmile", "/admin"),
        makeNonStaticOption("Receiver Status Dashboard", "send-dash", "/admin"),
        makeNonStaticOption("Value Sets", "value-sets", "/admin"),
        makeNonStaticOption("Validate", "validate", "/file-handler"),
    ];
    if (checkFlag(FeatureFlagName.USER_UPLOAD))
        pages.push(
            makeNonStaticOption("User Upload", "user-upload", "/file-handler")
        );
    if (checkFlag(FeatureFlagName.MESSAGE_TRACKER))
        pages.push(
            makeNonStaticOption(
                "Message Id Search",
                "message-tracker",
                "/admin"
            )
        );
    return <DropdownNav label={"Admin"} root={"/admin"} directories={pages} />;
};

export default DropdownNav;
