import { useResource } from "rest-hooks";
import { Link, NavLink } from "react-router-dom";
import { ButtonGroup, Table } from "@trussworks/react-uswds";
import React from "react";

import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import Spinner from "../Spinner";

import { DisplayMeta } from "./DisplayMeta";

interface OrgSettingsTableProps {
    orgname: string;
}

export function OrgSenderTable(props: OrgSettingsTableProps) {
    const orgSenderSettings: OrgSenderSettingsResource[] = useResource(
        OrgSenderSettingsResource.list(),
        { orgname: props.orgname }
    );

    return (
        <section
            id="orgsendersettings"
            className="grid-container margin-bottom-5"
        >
            <h2>
                Organization Sender Settings ({orgSenderSettings.length}){" - "}
                <Link
                    to={`/admin/revisionhistory/org/${props.orgname}/settingtype/sender`}
                >
                    History
                </Link>
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
                                <NavLink
                                    className="usa-button"
                                    to={`/admin/orgnewsetting/org/${props.orgname}/settingtype/sender`}
                                    key={`sender-create-link`}
                                >
                                    New
                                </NavLink>
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
                                    <DisplayMeta metaObj={eachOrgSetting} />
                                </td>
                                <td colSpan={2}>
                                    <ButtonGroup type="segmented">
                                        <NavLink
                                            className="usa-button"
                                            to={`/admin/orgsendersettings/org/${eachOrgSetting.organizationName}/sender/${eachOrgSetting.name}/action/edit`}
                                            key={`sender-edit-link-${eachOrgSetting.name}-${index}`}
                                        >
                                            Edit
                                        </NavLink>
                                        <NavLink
                                            className="usa-button"
                                            to={`/admin/orgsendersettings/org/${eachOrgSetting.organizationName}/sender/${eachOrgSetting.name}/action/clone`}
                                            key={`sender-clone-link-${eachOrgSetting.name}-${index}`}
                                        >
                                            Clone
                                        </NavLink>
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
