import {
    Button,
    Header,
    Menu,
    NavDropDownButton,
    PrimaryNav,
    Title,
} from "@trussworks/react-uswds";
import { useState } from "react";

import styles from "./ReportStreamNavbar.module.scss";

export const ReportStreamNavbar = ({
    blueVariant,
}: {
    blueVariant?: boolean;
}) => {
    const [openMenuItem, setOpenMenuItem] = useState<null | string>(null);
    const setMenu = (menuName: string) => {
        if (openMenuItem === menuName) {
            setOpenMenuItem(null);
        } else {
            setOpenMenuItem(menuName);
        }
    };

    const Dropdown = ({
        menuName,
        dropdownList,
    }: {
        menuName: string;
        dropdownList: React.ReactElement[];
    }) => {
        return (
            <>
                <NavDropDownButton
                    menuId={menuName.toLowerCase()}
                    isOpen={openMenuItem === menuName}
                    label={menuName}
                    onToggle={() => {
                        setMenu(menuName);
                    }}
                />
                <Menu
                    items={dropdownList}
                    isOpen={openMenuItem === menuName}
                    id={`${menuName}Dropdown`}
                />
            </>
        );
    };
    const testItemsMenu = [
        <Dropdown
            menuName="About"
            dropdownList={[
                <a href="#linkOne" key="one">
                    Current link
                </a>,
                <a href="#linkTwo" key="two">
                    Simple link Two
                </a>,
            ]}
        />,
        <Dropdown
            menuName="Getting started"
            dropdownList={[
                <a href="#linkOne" key="one">
                    Current link
                </a>,
                <a href="#linkTwo" key="two">
                    Simple link Two
                </a>,
            ]}
        />,
        <Dropdown
            menuName="Developer resources"
            dropdownList={[
                <a href="#linkOne" key="one">
                    Current link
                </a>,
                <a href="#linkTwo" key="two">
                    Simple link Two
                </a>,
            ]}
        />,
        <Dropdown
            menuName="Managing your connection"
            dropdownList={[
                <a href="#linkOne" key="one">
                    Current link
                </a>,
                <a href="#linkTwo" key="two">
                    Simple link Two
                </a>,
            ]}
        />,
        <Dropdown
            menuName="Support"
            dropdownList={[
                <a href="#linkOne" key="one">
                    Current link
                </a>,
                <a href="#linkTwo" key="two">
                    Simple link Two
                </a>,
            ]}
        />,
    ];
    return (
        <Header
            basic={true}
            className={
                blueVariant ? styles.NavbarBlueVariant : styles.NavbarDefault
            }
        >
            <div className="usa-nav-container">
                <div className="usa-navbar">
                    <Title>ReportStream</Title>
                </div>
                <PrimaryNav items={testItemsMenu}>
                    <div>
                        <Button outline type="button">
                            Login
                        </Button>
                        <Button type="button">Connect now</Button>
                    </div>
                </PrimaryNav>
            </div>
        </Header>
    );
};
