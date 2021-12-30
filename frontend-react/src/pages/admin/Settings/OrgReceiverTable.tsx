import { useResource } from "rest-hooks";

import OrgReceiverSettingsResource from "../../../resources/OrgReceiverSettingsResource";

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
                                {JSON.stringify(eachOrgSetting?.meta) || {}}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </section>
    );
}
