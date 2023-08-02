import React from "react";

import { RSReceiver } from "../../../config/endpoints/settings";

interface Props {
    /* REQUIRED
    A list of receiver services gathered by calling fetchReceivers() */
    receiverServices: RSReceiver[];

    /* REQUIRED
    The chosen receiver */
    active: string;

    /* REQUIRED
    A function passed in by the parent prop to sync chosen state
    This can be seen in-use by <DeliveriesTable>. The chosen state in sync'd
    and DeliveriesTable filters by the chosen receiver service */
    chosenCallback: (s: string) => void;
}

/*
    These are the options used to swap between various receiver services
*/
function ReceiversDropdown(props: Props) {
    return (
        <>
            <label className="usa-label text-bold" htmlFor="receivers-dropdown">
                Receiver service:
            </label>
            <select
                className="usa-select"
                id="receivers-dropdown"
                name="receivers-dropdown"
                defaultValue={props.active}
                onChange={(event) => props.chosenCallback(event.target.value)}
            >
                {props.receiverServices.map((receiver, idx) => (
                    <option key={`${receiver}.${idx}`} value={receiver.name}>
                        {receiver.name}
                    </option>
                ))}
            </select>
        </>
    );
}

export default function ReceiverServices({
    receiverServices,
    activeService,
    handleSetActive,
}: {
    receiverServices: RSReceiver[];
    activeService: RSReceiver | undefined;
    handleSetActive: (v: string) => void;
}) {
    return (
        <div className="flex-align-self-end padding-right-4">
            {receiverServices?.length > 1 ? (
                <ReceiversDropdown
                    receiverServices={receiverServices}
                    active={activeService?.name || ""}
                    chosenCallback={handleSetActive}
                />
            ) : (
                <p className="margin-bottom-0">
                    <strong>Receiver service: </strong>
                    {(receiverServices?.length &&
                        receiverServices[0].name.toUpperCase()) ||
                        ""}
                </p>
            )}
        </div>
    );
}
