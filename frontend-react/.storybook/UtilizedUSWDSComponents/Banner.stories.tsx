import React, { ReactElement } from "react";
import { GovBanner } from "@trussworks/react-uswds";

const Banner = (): ReactElement => {
    return <GovBanner aria-label="Official government website" />;
};

export default {
    title: "Components/Banner",
    component: GovBanner,
};

export const customBanner = (): React.ReactElement => <Banner />;
