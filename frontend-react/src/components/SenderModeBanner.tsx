import { ReactElement, useContext } from "react";
import { Alert } from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";

import useSenderMode from "../hooks/UseSenderMode";
import { SessionStorageContext } from "../contexts/SessionStorageContext";

const isNotActive = (val: string): boolean => {
    return val === "testing" || val === "inactive";
};

const SenderModeBanner = (): ReactElement | null => {
    const session = useContext(SessionStorageContext);
    const status = useSenderMode(session.values.org, session.values.senderName);
    const ref = "/getting-started/testing-facilities/overview";

    if (isNotActive(status)) {
        return (
            <Alert type="warning" slim>
                <b>Onboarding: </b> Your account is not yet sending data to your
                public health authority.{" "}
                <NavLink to={ref}>Learn more about onboarding.</NavLink>
            </Alert>
        );
    }

    return null;
};

export default SenderModeBanner;
