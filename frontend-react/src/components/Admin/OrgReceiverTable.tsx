import {
    Button,
    ButtonGroup,
    Modal,
    ModalFooter,
    ModalHeading,
    ModalRef,
    Table,
} from "@trussworks/react-uswds";
import DOMPurify from "dompurify";
import { useCallback, useRef, useState } from "react";
import { useResource } from "rest-hooks";

import { DisplayMeta } from "./DisplayMeta";
import {
    CheckSettingParams,
    CheckSettingResult,
    useCheckSettingsCmd,
} from "../../network/api/CheckSettingCmd";
import OrgReceiverSettingsResource from "../../resources/OrgReceiverSettingsResource";
import Spinner from "../Spinner";
import { USLink, USNavLink } from "../USLink";

interface OrgSettingsTableProps {
    orgname: string;
}

const DEFAULT_DATA: CheckSettingResult = {
    result: "",
    message: "",
};

export function OrgReceiverTable(props: OrgSettingsTableProps) {
    const { orgname: orgName } = props;
    const orgReceiverSettings: OrgReceiverSettingsResource[] = useResource(
        OrgReceiverSettingsResource.list(),
        props,
    );

    const { mutateAsync: doCheck, isPending } = useCheckSettingsCmd();
    const [checkResultData, setCheckResultData] =
        useState<CheckSettingResult>(DEFAULT_DATA);
    const modalRef = useRef<ModalRef>(null);
    const modalId = "checkSettingsModalId";
    const [clickedReceiver, setClickedReceiver] = useState("");

    // the "check" button in the list is clicked to display the modal
    const clickShowDialog = useCallback(
        (checkProps: CheckSettingParams) => {
            // clear sent back data
            setCheckResultData(DEFAULT_DATA);
            modalRef?.current?.toggleModal(undefined, true);
            setClickedReceiver(checkProps.receiverName);
        },
        [modalRef, setClickedReceiver],
    );

    // The "Start check" button is clicked in the modal to start the API call to do the check
    const clickDoCheckCmd = useCallback(async () => {
        try {
            setCheckResultData({
                result: "",
                message: "Starting... (this can take a while)",
            });
            const result = await doCheck({
                orgName,
                receiverName: clickedReceiver,
            });
            setCheckResultData(result);
        } catch (err: any) {
            // simulate a failure response
            setCheckResultData({
                result: "fail",
                message: err.toString(),
            });
        }
    }, [orgName, clickedReceiver, doCheck]);

    return (
        <section
            id="orgreceiversettings"
            className="grid-container margin-bottom-5"
        >
            <h2>
                Organization Receiver Settings ({orgReceiverSettings.length})
                {" - "}
                <USLink
                    href={`/admin/revisionhistory/org/${props.orgname}/settingtype/receiver`}
                >
                    History
                </USLink>
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
                                <USNavLink
                                    className="usa-button"
                                    href={`/admin/orgnewsetting/org/${props.orgname}/settingtype/receiver`}
                                    key={`receiver-create-link`}
                                >
                                    New
                                </USNavLink>
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
                                        <Button
                                            type="button"
                                            key={`receiver-row-checkcmd-${eachOrgSetting.name}-${index}`}
                                            data-receiver={eachOrgSetting.name}
                                            onClick={() =>
                                                clickShowDialog({
                                                    orgName: orgName,
                                                    receiverName: `${eachOrgSetting.name}`,
                                                })
                                            }
                                        >
                                            Check
                                        </Button>
                                        <USNavLink
                                            className="usa-button"
                                            href={`/admin/orgreceiversettings/org/${eachOrgSetting.organizationName}/receiver/${eachOrgSetting.name}/action/edit`}
                                            key={`receiver-edit-link-${eachOrgSetting.name}-${index}`}
                                        >
                                            Edit
                                        </USNavLink>
                                        <USNavLink
                                            className="usa-button"
                                            href={`/admin/orgreceiversettings/org/${eachOrgSetting.organizationName}/receiver/${eachOrgSetting.name}/action/clone`}
                                            key={`receiver-clone-link-${eachOrgSetting.name}-${index}`}
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
            <Modal
                isLarge={true}
                className="rs-admindash-modal rs-resend-modal"
                ref={modalRef}
                id={modalId}
                aria-labelledby={`${modalId}-heading`}
                aria-describedby={`${modalId}-description`}
            >
                <ModalHeading
                    id={`${modalId}-heading`}
                    data-testid={`${modalId}-heading`}
                >
                    This check will use the &apos;{clickedReceiver}&apos;
                    settings to connect to the receiver&apos;s server. <br />
                    No files will be sent. This feature ONLY supports SFTP and
                    REST receivers currently.
                </ModalHeading>
                <div className={"rs-admindash-modal-container"}>
                    <div className={"rs-resend-label"}>
                        Result:{" "}
                        <span
                            className={
                                checkResultData.result === "success"
                                    ? "success-all"
                                    : "failure-all"
                            }
                        >
                            {checkResultData.result}
                        </span>
                    </div>
                    <div
                        className="rs-editable-compare-base rs-resend-textarea-2x rs-resend-textarea-boarder"
                        contentEditable={false}
                        dangerouslySetInnerHTML={{
                            __html: DOMPurify.sanitize(checkResultData.message),
                        }}
                    />
                </div>
                <ModalFooter>
                    <ButtonGroup>
                        <Button
                            type="button"
                            outline
                            onClick={() =>
                                modalRef?.current?.toggleModal(undefined, false)
                            }
                        >
                            Cancel
                        </Button>
                        <Button
                            type="button"
                            disabled={isPending}
                            onClick={() => void clickDoCheckCmd()}
                        >
                            Start check
                        </Button>
                    </ButtonGroup>
                </ModalFooter>
            </Modal>
        </section>
    );
}
