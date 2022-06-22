import { ReactElement, useMemo } from "react";
import { IconWarning } from "@trussworks/react-uswds";
import { NavLink } from "react-router-dom";

import { useSessionContext } from "../contexts/SessionContext";
import useRequestConfig from "../hooks/network/UseRequestConfig";
import { API, createRequestConfig, Endpoint } from "../network/api/NewApi";
import { Sender } from "../network/api/OrgApi";
import { MemberType } from "../hooks/UseOktaMemberships";

const isNotActive = (val: string): boolean => {
    return val === "testing" || val === "inactive";
};

// Class is useless right now, no run-time type check
class RSSender {}
const SenderAPI: API = {
    resource: RSSender,
    baseUrl: "/api/settings/organizations",
    endpoints: new Map<string, Endpoint>([
        [
            "list",
            {
                url: "/:org/senders",
                methods: ["GET"],
            },
        ],
        [
            "detail",
            {
                url: "/:org/senders/:sender",
                methods: ["GET"],
            },
        ],
    ]),
};

const useSenderResource = () => {
    /* Access the session. */
    const { memberships, oktaToken } = useSessionContext();
    /* Create a stable config reference with useMemo(). */
    const config = useMemo(
        () =>
            createRequestConfig<{ org: string; sender: string }>(
                SenderAPI,
                "detail",
                "GET",
                oktaToken?.accessToken,
                memberships.state.active?.parsedName,
                {
                    org: memberships.state.active?.parsedName || "",
                    sender: memberships.state.active?.senderName || "default",
                }
            ),
        /* Note: we DO want to update config ONLY when these values update. If the linter
         * yells about a value you don't want to add, add an eslint-ignore comment. */
        [oktaToken?.accessToken, memberships.state.active]
    );
    /* Pass the stable config into the consumer and cast the response with types. */
    const {
        data: sender,
        error,
        loading,
    } = useRequestConfig(config) as {
        data: Sender; // Ideally we can use our resource class instead of interfaces.
        error: string;
        loading: boolean;
    };
    /* Finally, return the values from the hook. */
    return {
        sender,
        error,
        loading,
    };
};

const BannerContent = () => {
    const { sender, error, loading } = useSenderResource();
    const path = "/getting-started/testing-facilities/overview";
    if (!loading && error === "" && isNotActive(sender?.customerStatus)) {
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
    const { memberships } = useSessionContext();

    if (memberships.state.active?.memberType === MemberType.SENDER) {
        return <BannerContent />;
    }
    return null;
};

export default SenderModeBanner;
