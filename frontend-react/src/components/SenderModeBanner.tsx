import { ReactElement, useContext } from "react";
import { NavLink } from "react-router-dom";

import useSenderMode from "../hooks/UseSenderMode";
import { SessionContext } from "../contexts/SessionContext";

const isNotActive = (val: string): boolean => {
    return val === "testing" || val === "inactive";
};

const SenderModeBanner = (): ReactElement | null => {
    const session = useContext(SessionContext);
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
                            <svg
                                className="usa-icon"
                                aria-hidden="true"
                                focusable="false"
                                role="img"
                            >
                                <use xlinkHref="/assets/img/sprite.svg#warning" />
                            </svg>
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
