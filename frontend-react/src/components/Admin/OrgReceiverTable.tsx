import { useResource } from "rest-hooks";
import { Link, NavLink } from "react-router-dom";
import { ButtonGroup, Table } from "@trussworks/react-uswds";
import React from "react";

import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import Spinner from "../Spinner";

import { DisplayMeta } from "./DisplayMeta";

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
                {" - "}
                <Link
                    to={`/admin/revisionhistory/org/${props.orgname}/settingtype/receiver`}
                >
                    History
                </Link>
            </h2>
            {!orgReceiverSettings ? (
                <Spinner />
            ) : (
                <Table
                    key="orgreceiversettingstable"
                    aria-label="Organization Receivers"
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
                            <th scope="col">
                                <NavLink
                                    className="usa-button"
                                    to={`/admin/orgnewsetting/org/${props.orgname}/settingtype/receiver`}
                                    key={`receiver-create-link`}
                                >
                                    New
                                </NavLink>
                            </th>
                        </tr>
                    </thead>
                    <tbody id="tBodyOrgReceiver" className="font-mono-2xs">
                        {orgReceiverSettings.map((eachOrgSetting, index) => (
                            <tr
                                key={`receiver-row-${eachOrgSetting.name}-${index}`}
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
                                            to={`/admin/orgreceiversettings/org/${eachOrgSetting.organizationName}/receiver/${eachOrgSetting.name}/action/edit`}
                                            key={`receiver-edit-link-${eachOrgSetting.name}-${index}`}
                                        >
                                            Edit
                                        </NavLink>
                                        <NavLink
                                            className="usa-button"
                                            to={`/admin/orgreceiversettings/org/${eachOrgSetting.organizationName}/receiver/${eachOrgSetting.name}/action/clone`}
                                            key={`receiver-clone-link-${eachOrgSetting.name}-${index}`}
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
