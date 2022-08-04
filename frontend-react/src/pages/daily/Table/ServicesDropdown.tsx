import { Dropdown } from "@trussworks/react-uswds";

import { RSReceiver } from "../../../network/api/Organizations/Receivers";

interface Props {
    /* REQUIRED
    A list of senders gathered by calling getListOfSenders() */
    services: RSReceiver[];

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
        <div className="grid-container">
            <Dropdown id="services-dropdown" name="services-dropdown">
                {props.services.map((service, idx) => (
                    <option
                        onClick={props.chosenCallback(service)}
                        key={`${service}.${idx}`}
                    >
                        {service.name}
                    </option>
                ))}
            </Dropdown>
        </div>
    );
}

export default ServicesDropdown;
