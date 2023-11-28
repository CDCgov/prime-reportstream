import { ReactElement } from "react";
import { Icon } from "@trussworks/react-uswds";

import useSenderResource from "../../hooks/UseSenderResource";
import { USLink } from "../../components/USLink";
import type { RSSender } from "../../config/endpoints/settings";

const isNotActive = (val: string | undefined): boolean => {
    return val === "testing" || val === "inactive";
};

export interface SenderModeBannerBaseProps extends React.PropsWithChildren {
    customerStatus?: RSSender["customerStatus"];
}

export function SenderModeBannerBase({
    children,
    customerStatus,
}: SenderModeBannerBaseProps) {
    if (isNotActive(customerStatus)) {
        return (
            <section>
                <header className="usa-banner__header bg-yellow">
                    <div className="usa-banner__inner">
                        <div className="grid-col-auto margin-right-1">
                            <Icon.Warning />
                        </div>
                        <div className="grid-col-fill tablet:grid-col-auto">
                            <b>Onboarding: </b> Your account is not yet sending
                            data to your public health authority.{" "}
                            <USLink href="/getting-started/testing-facilities/overview">
                                Learn more about onboarding.
                            </USLink>
                            {children}
                        </div>
                    </div>
                </header>
            </section>
        );
    }

    return null;
}

const SenderModeBanner = (
    props: React.PropsWithChildren,
): ReactElement | null => {
    const { data } = useSenderResource();
    return (
        <SenderModeBannerBase
            customerStatus={data?.customerStatus}
            {...props}
        />
    );
};

export default SenderModeBanner;
