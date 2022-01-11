import { useResource } from "rest-hooks";
import { NavLink } from "react-router-dom";

import OrgReceiverSettingsResource from "../../../resources/OrgReceiverSettingsResource";

// const theme = {
//     scheme: "monokai",
//     author: "wimer hazenberg (http://www.monokai.nl)",
//     base00: "#272822",
//     base01: "#383830",
//     base02: "#49483e",
//     base03: "#75715e",
//     base04: "#a59f85",
//     base05: "#f8f8f2",
//     base06: "#f5f4f1",
//     base07: "#f9f8f5",
//     base08: "#f92672",
//     base09: "#fd971f",
//     base0A: "#f4bf75",
//     base0B: "#a6e22e",
//     base0C: "#a1efe4",
//     base0D: "#66d9ef",
//     base0E: "#ae81ff",
//     base0F: "#cc6633",
// };

interface OrgSettingsTableProps {
    orgname: string;
}

export function OrgReceiverTable(props: OrgSettingsTableProps) {
    const orgReceiverSettings: OrgReceiverSettingsResource[] = useResource(
        OrgReceiverSettingsResource.list(),
        { orgname: props.orgname }
    );

    return (
        <section
            id="orgreceiversettings"
            className="grid-container margin-bottom-5"
        >
            <h2>
                Organization Receiver Settings ({orgReceiverSettings.length})
            </h2>
            <table
                id="orgreceiversettingstable"
                className="usa-table usa-table--borderless prime-table"
                aria-label="Organization Receivers"
            >
                <thead>
                    <tr>
                        <th scope="col">Name</th>
                        <th scope="col">Org Name</th>
                        <th scope="col">Topic</th>
                        <th scope="col">Status</th>
                        <th scope="col">Meta</th>
                    </tr>
                </thead>
                <tbody id="tBodyOrgReceiver" className="font-mono-2xs">
                    {orgReceiverSettings.map((eachOrgSetting) => (
                        <tr key={eachOrgSetting.name}>
                            <td>{eachOrgSetting.name}</td>
                            <td>{eachOrgSetting?.organizationName || "-"}</td>
                            <td>{eachOrgSetting.topic || ""}</td>
                            <td>{eachOrgSetting.customerStatus || ""}</td>
                            <td>
                                {/*<JSONTree data={eachOrgSetting.translation} theme={theme} invertTheme={true}/>*/}
                                {/*<code>*/}
                                {/*    { JSON.stringify(eachOrgSetting?.meta,null,"\n\t") || {}}*/}
                                {/*</code>*/}
                                <NavLink
                                    to={`/admin/orgreceiversettings/${eachOrgSetting.organizationName}/${eachOrgSetting.name}`}
                                    key={eachOrgSetting.name}
                                    className="usa-link"
                                >
                                    Edit {eachOrgSetting.name}
                                </NavLink>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </section>
    );
}
