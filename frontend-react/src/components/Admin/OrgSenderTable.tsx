import { useController, useResource } from "rest-hooks";
import { NavLink } from "react-router-dom";
import { Button, ModalRef, ButtonGroup, Table } from "@trussworks/react-uswds";
import React, { useRef, useState } from "react";

import OrgSenderSettingsResource from "../../resources/OrgSenderSettingsResource";
import { showAlertNotification, showError } from "../AlertNotifications";

import { ConfirmDeleteSettingModal } from "./AdminModal";
import { DisplayMeta } from "./DisplayMeta";

interface OrgSettingsTableProps {
    orgname: string;
}

export function OrgSenderTable(props: OrgSettingsTableProps) {
    const orgSenderSettings: OrgSenderSettingsResource[] = useResource(
        OrgSenderSettingsResource.list(),
        { orgname: props.orgname }
    );
    const { fetch: fetchController } = useController();
    const [deleteItemId, SetDeleteItemId] = useState("");
    const modalRef = useRef<ModalRef>(null);

    const doDeleteSetting = async () => {
        try {
            await fetchController(OrgSenderSettingsResource.deleteSetting(), {
                orgname: props.orgname,
                sendername: deleteItemId,
            });

            // instead of refetching, just remove from list
            const index = orgSenderSettings.findIndex(
                (item) => item.name === deleteItemId
            );
            if (index >= 0) {
                orgSenderSettings.splice(index, 1);
            }
            showAlertNotification(
                "success",
                `Item '${deleteItemId}' has been deleted`
            );
            return true;
        } catch (e: any) {
            console.trace(e);
            showError(
                `Deleting item '${deleteItemId}' failed. ${e.toString()}`
            );
            return false;
        }
    };

    const ShowDeleteConfirm = (itemId: string) => {
        SetDeleteItemId(itemId);
        modalRef?.current?.toggleModal(undefined, true);
    };

    return (
        <section
            id="orgsendersettings"
            className="grid-container margin-bottom-5"
        >
            <h2>Organization Sender Settings ({orgSenderSettings.length})</h2>
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
                        <tr key={`sender-row-${eachOrgSetting.name}-${index}`}>
                            <td>{eachOrgSetting.name}</td>
                            <td>{eachOrgSetting?.organizationName || "-"}</td>
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
                                    <Button
                                        type={"button"}
                                        onClick={() =>
                                            ShowDeleteConfirm(
                                                eachOrgSetting.name
                                            )
                                        }
                                    >
                                        Delete
                                    </Button>
                                </ButtonGroup>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </Table>
            <ConfirmDeleteSettingModal
                uniquid={deleteItemId}
                onConfirm={doDeleteSetting}
                modalRef={modalRef}
            >
                Delete
            </ConfirmDeleteSettingModal>
        </section>
    );
}
