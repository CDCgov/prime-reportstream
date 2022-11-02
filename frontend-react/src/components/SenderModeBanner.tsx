import { ReactElement } from "react";
import { IconWarning } from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";

import { useSessionContext } from "../contexts/SessionContext";
import { MemberType } from "../hooks/UseOktaMemberships";
import { useSenderResource } from "../hooks/UseSenderResource";

const isNotActive = (val: string | undefined): boolean => {
    return val === "testing" || val === "inactive";
};

const BannerContent = () => {
    const { senderDetail: sender, senderIsLoading: loading } =
        useSenderResource();
    const path = "/getting-started/testing-facilities/overview";
    if (!loading && isNotActive(sender?.customerStatus)) {
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

const SenderModeBanner = (): ReactElement | null => {
    const { activeMembership } = useSessionContext();

    if (activeMembership?.memberType === MemberType.SENDER) {
        return <BannerContent />;
    }
    return null;
};

export default SenderModeBanner;
