import { useState } from "react";
import { useResource } from "rest-hooks";
import { TextInput } from "@trussworks/react-uswds";

import OrgSettingsResource from "../../../resources/OrgSettingsResource";

interface OrgSettingsTableProps {}

export function OrgSettingsTable(props: OrgSettingsTableProps) {
    const orgSettings: OrgSettingsResource[] = useResource(
        OrgSettingsResource.list(),
        {}
    );
    const [filter, setFilter] = useState("");

    return (
        <section id="orgsettings" className="grid-container margin-bottom-5">
            <h2>Facilities reporting ({orgSettings.length})</h2>
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
            <table
                id="facilitiestable"
                className="usa-table usa-table--borderless prime-table"
                aria-label="Facilities included in this report"
            >
                <thead>
                    <tr>
                        <th scope="col">Org Name</th>
                        <th scope="col">Description</th>
                        <th scope="col">Jurisdiction</th>
                        <th scope="col">State</th>
                        <th scope="col">County</th>
                    </tr>
                </thead>
                <tbody id="tBodyFac" className="font-mono-2xs">
                    {orgSettings
                        .filter((eachOrgSetting) =>
                            eachOrgSetting.filterMatch(filter)
                        )
                        .map((eachOrgSetting) => (
                            <tr key={eachOrgSetting.name}>
                                <td>{eachOrgSetting.name}</td>
                                <td>{eachOrgSetting?.description || "-"}</td>
                                <td>{eachOrgSetting.jurisdiction || ""}</td>
                                <td>{eachOrgSetting.stateCode || ""}</td>
                                <td>{eachOrgSetting.countyName || ""}</td>
                            </tr>
                        ))}
                </tbody>
            </table>
        </section>
    );
}
