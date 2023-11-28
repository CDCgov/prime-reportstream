import { NavDropDownButton, Menu } from "@trussworks/react-uswds";

export interface DropdownProps extends React.PropsWithChildren {
    onToggle: (name: string) => void;

    menuName: string;
    dropdownList: React.ReactElement[];
    currentMenuName?: string;
}

function Dropdown({
    menuName,
    dropdownList,
    currentMenuName,
    onToggle,
}: DropdownProps) {
    return (
        <>
            <NavDropDownButton
                menuId={menuName.toLowerCase()}
                isOpen={currentMenuName === menuName}
                isCurrent={currentMenuName === menuName}
                label={menuName}
                onToggle={() => onToggle(menuName)}
            />
            <Menu
                items={dropdownList}
                isOpen={currentMenuName === menuName}
                id={`${menuName}Dropdown`}
            />
        </>
    );
}

export default Dropdown;
