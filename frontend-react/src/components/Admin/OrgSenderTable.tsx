import { useResource } from "rest-hooks";
import { ButtonGroup, Table } from "@trussworks/react-uswds";

import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import Spinner from "../Spinner";
import { USLink, USNavLink } from "../USLink";
import MetaDisplay from "../../shared/MetaDisplay/MetaDisplay";

interface OrgSettingsTableProps {
    orgId: string;
}

export function OrgSenderTable(props: OrgSettingsTableProps) {
    const orgSenderSettings: OrgSenderSettingsResource[] = useResource(
        OrgSenderSettingsResource.list(),
        { orgId: props.orgId },
    );

    return (
        <section
            id="orgsendersettings"
            className="grid-container margin-bottom-5"
        >
            <h2>
                Organization Sender Settings ({orgSenderSettings.length}){" - "}
                <USLink
                    href={`/admin/organizations/${props.orgId}/sender/settingType/history`}
                >
                    History
                </USLink>
            </h2>
            {!orgSenderSettings ? (
                <Spinner />
            ) : (
                <Table
                    key="orgsendersettingstable"
                    aria-label="Organization Senders"
                    striped
                    fullWidth
                >
                    <thead>
                        <tr>
                            <th scope="col">Name</th>
                            <th scope="col">Org Name</th>
                            <th scope="col">Topic</th>
                            <th scope="col">Status</th>
                            <th scope="col">Meta</th>
                            <th scope="col">Action</th>
                            <th scope="col" align="right">
                                <USNavLink
                                    className="usa-button"
                                    href={`/admin/organizations/${props.orgId}/sender/new`}
                                    key={`sender-create-link`}
                                >
                                    New
                                </USNavLink>
                            </th>
                        </tr>
                    </thead>
                    <tbody id="tBodyOrgSender" className="font-mono-2xs">
                        {orgSenderSettings.map((eachOrgSetting, index) => (
                            <tr
                                key={`sender-row-${eachOrgSetting.name}-${index}`}
                            >
                                <td>{eachOrgSetting.name}</td>
                                <td>
                                    {eachOrgSetting?.organizationName || "-"}
                                </td>
                                <td>{eachOrgSetting.topic || ""}</td>
                                <td>{eachOrgSetting.customerStatus || ""}</td>
                                <td>
                                    <MetaDisplay metaObj={eachOrgSetting} />
                                </td>
                                <td colSpan={2}>
                                    <ButtonGroup type="segmented">
                                        <USNavLink
                                            className="usa-button"
                                            href={`/admin/organizations/${eachOrgSetting.organizationName}/sender/${eachOrgSetting.name}/edit`}
                                            key={`sender-edit-link-${eachOrgSetting.name}-${index}`}
                                        >
                                            Edit
                                        </USNavLink>
                                        <USNavLink
                                            className="usa-button"
                                            href={`/admin/organizations/${eachOrgSetting.organizationName}/sender/new/${eachOrgSetting.name}`}
                                            key={`sender-clone-link-${eachOrgSetting.name}-${index}`}
                                        >
                                            Clone
                                        </USNavLink>
                                    </ButtonGroup>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </Table>
            )}
        </section>
    );
}
