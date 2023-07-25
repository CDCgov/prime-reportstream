import React from "react";

import { RSReceiver } from "../../../config/endpoints/settings";

interface Props {
    /* REQUIRED
    A list of senders gathered by calling getListOfSenders() */
    receivers: RSReceiver[];

    /* REQUIRED
    The chosen service */
    active: string;

    /* REQUIRED
    A function passed in by the parent prop to sync chosen state
    This can be seen in-use by <DeliveriesTable>. The chosen state in sync'd
    and DeliveriesTable filters by the chosen sender */
    chosenCallback: (s: string) => void;
}

/*
    These are the buttons used to swap between various senders of data
    to see only reports sent by individual senders populated on their
    list
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
                {props.receivers.map((receiver, idx) => (
                    <option key={`${receiver}.${idx}`} value={receiver.name}>
                        {receiver.name}
                    </option>
                ))}
            </select>
        </>
    );
}

export default function ReceiverServices({
    receivers,
    activeService,
    handleSetActive,
}: {
    receivers: RSReceiver[];
    activeService: RSReceiver | undefined;
    handleSetActive: (v: string) => void;
}) {
    return (
        <div className="flex-align-self-end padding-right-4">
            {receivers?.length > 1 ? (
                <ReceiversDropdown
                    receivers={receivers}
                    active={activeService?.name || ""}
                    chosenCallback={handleSetActive}
                />
            ) : (
                <p className="margin-bottom-0">
                    <strong>Receiver service: </strong>
                    {(receivers?.length && receivers[0].name.toUpperCase()) ||
                        ""}
                </p>
            )}
        </div>
    );
}
