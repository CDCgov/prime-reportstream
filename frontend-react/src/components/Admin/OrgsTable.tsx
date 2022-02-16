import { useState } from "react";
import { useResource } from "rest-hooks";
import { Button, ButtonGroup, TextInput, Table } from "@trussworks/react-uswds";
import { useHistory } from "react-router-dom";

import OrgSettingsResource from "../../resources/OrgSettingsResource";
import { useGlobalContext } from "../GlobalContextProvider";

export function OrgsTable() {
    const orgs: OrgSettingsResource[] = useResource(
        OrgSettingsResource.list(),
        {}
    ).sort((a, b) => a.name.localeCompare(b.name));
    const [filter, setFilter] = useState("");
    const { updateOrganization } = useGlobalContext();
    const history = useHistory();

    const handleSelectOrgClick = (orgName: string) => {
        updateOrganization(orgName);
        if (window.location.pathname.includes("/report-details")) {
            history.push("/daily-data");
        }
        window.location.reload();
    };

    const handleEditOrgClick = (orgName: string) => {
        // editing... maybe we should keep current org in sync? Switch to the "safe org"?
        updateOrganization("PrimeAdmins");
        history.push(`/admin/orgsettings/org/${orgName}`);
    };

    return (
        <section id="orgsettings" className="grid-container margin-bottom-5">
            <h2>Organizations ({orgs.length})</h2>
            <div>
                Filter:
                <TextInput
                    id="input-filter"
                    name="input-filter"
                    type="text"
                    autoComplete={"none"}
                    aria-autocomplete={"none"}
                    onChange={(evt) => setFilter(evt.target.value)}
                />
            </div>
            <Table
                key="orgsettingstable"
                aria-label="Organizations"
                striped
                fullWidth
            >
                <thead>
                    <tr>
                        <th scope="col">Name</th>
                        <th scope="col">Description</th>
                        <th scope="col">Jurisdiction</th>
                        <th scope="col">State</th>
                        <th scope="col">County</th>
                        <th scope="col"> </th>
                    </tr>
                </thead>
                <tbody id="tBodyFac" className="font-mono-2xs">
                    {orgs
                        .filter((eachOrg) => eachOrg.filterMatch(filter))
                        .map((eachOrg) => (
                            <tr key={`sender-row-${eachOrg.name}`}>
                                <td className="font-heading-sm">
                                    {eachOrg.name}
                                </td>
                                <td>{eachOrg?.description || "-"}</td>
                                <td>{eachOrg.jurisdiction || ""}</td>
                                <td>{eachOrg.stateCode || ""}</td>
                                <td>{eachOrg.countyName || ""}</td>
                                <td>
                                    <ButtonGroup type="segmented">
                                        <Button
                                            key={`${eachOrg.name}_select`}
                                            onClick={() =>
                                                handleSelectOrgClick(
                                                    `${eachOrg.name}`
                                                )
                                            }
                                            type="button"
                                            size="small"
                                            className="padding-1"
                                        >
                                            Activate
                                        </Button>
                                        <Button
                                            key={`${eachOrg.name}_edit`}
                                            onClick={() =>
                                                handleEditOrgClick(
                                                    `${eachOrg.name}`
                                                )
                                            }
                                            type="button"
                                            size="small"
                                            className="padding-1"
                                        >
                                            Edit...
                                        </Button>
                                    </ButtonGroup>
                                </td>
                            </tr>
                        ))}
                </tbody>
            </Table>
        </section>
    );
}
