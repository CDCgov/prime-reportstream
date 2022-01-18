import { useController, useResource } from "rest-hooks";
import { NavLink } from "react-router-dom";
import { Button, ModalRef, ButtonGroup, Table } from "@trussworks/react-uswds";
import { useRef, useState } from "react";

import OrgSenderSettingsResource from "../../../resources/OrgSenderSettingsResource";
import { ConfirmDeleteSettingModal } from "../../../components/Admin/AdminModal";
import { showAlertNotification, showError, showNotification } from "../../../components/AlertNotifications";

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

    const doDeleteSetting = () => {
        try {
            debugger;
            console.log(deleteItemId);
            // delete deleteItemId;
            showAlertNotification("success", `Item '${deleteItemId}' has been deleted`);
        } catch (e) {
            // @ts-ignore
            showError(`Deleting item '${deleteItemId}' failed. ${e.toString()}`);
        }

    };

    const ShowDeleteConfirm = (itemId: string) => {
        SetDeleteItemId(itemId);
        modalRef?.current?.toggleModal(undefined, true);
    };

    // function testUpdate(setting: OrgSenderSettingsResource) {
    //     setting.topic = "COVID-2023";
    //     const testData = JSON.stringify(setting);
    //     console.log(testData);
    //     fetchController(
    //         OrgSenderSettingsResource.update(),
    //         { orgname: setting.organizationName, sendername: setting.name },
    //         testData
    //     );
    // }

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
                    {orgSenderSettings.map((eachOrgSetting, index) => (
                        <tr key={`sender-row-${eachOrgSetting.name}-${index}`}>
                            <td>{eachOrgSetting.name}</td>
                            <td>{eachOrgSetting?.organizationName || "-"}</td>
                            <td>{eachOrgSetting.topic || ""}</td>
                            <td>{eachOrgSetting.customerStatus || ""}</td>
                            <td>
                                {JSON.stringify(eachOrgSetting?.meta) || {}}
                            </td>
                            <td>
                                <ButtonGroup type="segmented">
                                    <NavLink
                                        className="usa-button"
                                        to={`/admin/orgsendersettings/org/${eachOrgSetting.organizationName}/sender/${eachOrgSetting.name}/action/edit`}
                                        key={`sender-edit-link-${eachOrgSetting.name}-${index}`}
                                    >
                                        Edit
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
                uniquid={"deletesenderdata"}
                onConfirm={doDeleteSetting}
                modalRef={modalRef}
            >
                Delete
            </ConfirmDeleteSettingModal>
        </section>
    );
}
