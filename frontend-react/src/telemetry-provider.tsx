import React, { Fragment, useEffect } from "react";
import { useHistory } from "react-router-dom";

import { ai } from "./TelemetryService";

const TelemetryProvider: React.FC = ({ children }) => {
    const history = useHistory();

    useEffect(() => {
        ai.initialize(history);
    }, [history]);

    return <Fragment>{children}</Fragment>;
};

export default TelemetryProvider;
