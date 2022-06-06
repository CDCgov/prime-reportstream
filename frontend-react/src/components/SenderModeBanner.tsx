import { ReactElement } from "react";
import { IconWarning } from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";

import useSenderMode from "../hooks/UseSenderMode";
import { useSessionContext } from "../contexts/SessionContext";

const isNotActive = (val: string): boolean => {
    return val === "testing" || val === "inactive";
};

const SenderModeBanner = (): ReactElement | null => {
    const session = useSessionContext();
    const status = useSenderMode(
        session.store.values.org,
        session.store.values.senderName
    );
    const path = "/getting-started/testing-facilities/overview";

    if (isNotActive(status)) {
        return (
            <section>
                <header className="usa-banner__header bg-yellow">
                    <div className="usa-banner__inner">
                        <div className="grid-col-auto margin-right-1">
                            <IconWarning />
                        </div>
                        <div className="grid-col-fill tablet:grid-col-auto">
                            <b>Onboarding: </b> Your account is not yet sending
                            data to your public health authority.{" "}
                            <NavLink to={path}>
                                Learn more about onboarding.
                            </NavLink>
                        </div>
                    </div>
                </header>
            </section>
        );
    }

    return null;
};

export default SenderModeBanner;
