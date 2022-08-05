import { Dropdown } from "@trussworks/react-uswds";

import { RSReceiver } from "../../../network/api/Organizations/Receivers";

interface Props {
    /* REQUIRED
    A list of senders gathered by calling getListOfSenders() */
    services: RSReceiver[];

    /* REQUIRED
    The chosen service */
    active: string;

    /* REQUIRED
    A function passed in by the parent prop to sync chosen state
    This can be seen in-use by <ReportsTable>. The chosen state in sync'd
    and ReportsTable filters by the chosen sender */
    chosenCallback: Function;
}

/*
    These are the buttons used to swap between various senders of data
    to see only reports sent by individual senders populated on their
    list
*/
function ServicesDropdown(props: Props) {
    return (
        <Dropdown
            id="services-dropdown"
            name="services-dropdown"
            value={props.active}
            onChange={(event) => props.chosenCallback(event.target.value)}
        >
            {props.services.map((service, idx) => (
                <option key={`${service}.${idx}`} value={service.name}>
                    {service.name}
                </option>
            ))}
        </Dropdown>
    );
}

export default ServicesDropdown;
