import { useOktaAuth } from "@okta/okta-react";
import { Dropdown } from "@trussworks/react-uswds";
import { CSSProperties, useState } from "react";
import { useResource } from "rest-hooks";
import OrganizationResource from "../../resources/OrganizationResource";
import { useGlobalContext } from "../GlobalContextProvider";

const OrganizationDropdown = () => {

    const [org, setOrg] = useState("md-phd")
    const { updateOrganization } = useGlobalContext();

    // const { authState } = useOktaAuth()
    // const myOrgName = authState!.accessToken?.claims.organization.find(o => !o.toLowerCase().includes('sender'))

    let orgs = useResource(OrganizationResource.list(), {
        sortBy: undefined,
    }).sort((a, b) => a.name.localeCompare(b.name));

    let setValue = (e: any) => {
        //TODO: change org context for user
        setOrg(e)
        updateOrganization(e)
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