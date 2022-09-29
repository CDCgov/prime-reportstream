import React, { PropsWithChildren, useEffect } from "react";

import { ai } from "./TelemetryService";

const TelemetryProvider = ({ children }: PropsWithChildren<{}>) => {
    useEffect(() => {
        ai.initialize();
    }, []);

    return <>children</>;
};

export default TelemetryProvider;
