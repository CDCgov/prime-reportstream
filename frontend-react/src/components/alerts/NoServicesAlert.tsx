import React from "react";

import { StaticAlert } from "../StaticAlert";
import { capitalizeFirst } from "../../utils/misc";

interface NoServicesBannerProps {
    action: string;
    organization?: string;
    serviceType?: "sender" | "receiver";
}

export const NoServicesBanner = ({
    action,
    organization,
    serviceType,
}: NoServicesBannerProps) => {
    return (
        <StaticAlert
            type={"error"}
            heading={`${capitalizeFirst(action)} unavailable`}
            message={`No valid ${serviceType || "service"} found for ${
                organization || "your organization"
            }`}
        />
    );
};
