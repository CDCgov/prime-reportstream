import { Dropdown } from "@trussworks/react-uswds";
import { CSSProperties, useState } from "react";
import { useResource } from "rest-hooks";
import OrganizationResource from "../../resources/OrganizationResource";
import { GLOBAL_STORAGE_KEYS, useGlobalContext } from "../GlobalContextProvider";

const OrganizationDropdown = () => {
    const [org, setOrg] = useState(localStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || "")
    const { updateOrganization } = useGlobalContext();

    let orgs = useResource(OrganizationResource.list(), {
        sortBy: undefined,
    }).sort((a, b) => a.name.localeCompare(b.name));

    let setValue = (e: any) => {
        //TODO: change org context for user
        setOrg(e)
        updateOrganization(e)
        window.location.reload()
    };
    const dropdownStyles: CSSProperties = {
        maxWidth: "200px",
        margin: "0 2rem"
    }

    return (
        <Dropdown
            id="input-dropdown"
            name="input-dropdown"
            style={dropdownStyles}
            defaultValue={org}
            onChange={(e) => setValue(e.target.value)}
        >
            {orgs.map((org) => (
                <option key={org.name} value={org.name}>
                    {org.name}
                </option>
            ))}
        </Dropdown>
    );
};

export { OrganizationDropdown }