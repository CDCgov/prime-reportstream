import { useCallback } from "react";

import { RSReceiver } from "../../../network/api/Organizations/Receivers";

interface Props {
    /* REQUIRED
    A list of senders gathered by calling getListOfSenders() */
    services: RSReceiver[] | string[];

    /* REQUIRED
    The chosen service */
    active: string;

    /* REQUIRED
    A function passed in by the parent prop to sync chosen state
    This can be seen in-use by <ReportsTable>. The chosen state in sync'd
    and ReportsTable filters by the chosen sender */
    chosenCallback: (s: string) => void;
}

/*
    These are the buttons used to swap between various senders of data
    to see only reports sent by individual senders populated on their
    list
*/
function ServicesDropdown(props: Props) {
    const name = useCallback((service: string | RSReceiver) => {
        if (service instanceof RSReceiver) return service.name;
        return service;
    }, []);
    return (
        <select
            className="usa-select"
            id="services-dropdown"
            name="services-dropdown"
            defaultValue={props.active}
            onChange={(event) => props.chosenCallback(event.target.value)}
        >
            {props.services.map((service, idx) => (
                <option key={`${service}.${idx}`} value={name(service)}>
                    {name(service)}
                </option>
            ))}
        </select>
    );
}

export default ServicesDropdown;
