import { Button } from "@trussworks/react-uswds";
import {useController, useResource} from "rest-hooks";

import OrgSenderSettingsResource from "../../../resources/OrgSenderSettingsResource";

interface OrgSettingsTableProps {
    orgname: string;
}

export function OrgSenderTable(props: OrgSettingsTableProps) {
    const orgSenderSettings: OrgSenderSettingsResource[] = useResource(
        OrgSenderSettingsResource.list(),
        { orgname: props.orgname }
    );
    const { fetch } = useController();
    function testUpdate(setting: OrgSenderSettingsResource) {
        debugger;
        setting.topic = "COVID-2023";
        const testData = JSON.stringify(setting);
        console.log(testData);
        fetch(OrgSenderSettingsResource.update(), { orgname: setting.organizationName, sendername: setting.name }, testData);
    }

    return (
        <section
            id="orgsendersettings"
            className="grid-container margin-bottom-5"
        >
            <h2>Organization Sender Settings ({orgSenderSettings.length})</h2>
            <table
                id="orgsendersettingstable"
                className="usa-table usa-table--borderless prime-table"
                aria-label="Organization Senders"
            >
                <thead>
                    <tr>
                        <th scope="col">Name</th>
                        <th scope="col">Org Name</th>
                        <th scope="col">Topic</th>
                        <th scope="col">Status</th>
                        <th scope="col">Meta</th>
                        <th scope="col">Action</th>
                    </tr>
                </thead>
                <tbody id="tBodyOrgSender" className="font-mono-2xs">
                    {orgSenderSettings.map((eachOrgSetting) => (
                        <tr key={eachOrgSetting.name}>
                            <td>{eachOrgSetting.name}</td>
                            <td>{eachOrgSetting?.organizationName || "-"}</td>
                            <td>{eachOrgSetting.topic || ""}</td>
                            <td>{eachOrgSetting.customerStatus || ""}</td>
                            <td>
                                {JSON.stringify(eachOrgSetting?.meta) || {}}
                            </td>
                            <td><Button type="button" name="test" onClick={ (e) => testUpdate(eachOrgSetting)}>UPDATE!</Button></td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </section>
    );
}
