import { Dropdown } from "@trussworks/react-uswds";
import { CSSProperties, useState } from "react";
import { useHistory } from "react-router";
import { useResource } from "rest-hooks";
import OrganizationResource from "../../resources/OrganizationResource";
import { GLOBAL_STORAGE_KEYS, useGlobalContext } from "../GlobalContextProvider";

const OrganizationDropdown = () => {
    const [org, setOrg] = useState(localStorage.getItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG) || "");
    const history = useHistory();
    const { updateOrganization } = useGlobalContext();

    let orgs = useResource(OrganizationResource.list(), {
        sortBy: undefined,
    }).sort((a, b) => a.name.localeCompare(b.name));

    let setValue = (e: any) => {
        setOrg(e)
        updateOrganization(e)
        if (window.location.pathname.includes('/report-details')) {
            history.push('/daily-data')
        }
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
            {orgs.map((orgItem) => (
                <option key={orgItem.name} value={orgItem.name}>
                    {orgItem.name}
                </option>
            ))}
        </Dropdown>
    );
};

export { OrganizationDropdown }