import { ReactElement, useContext } from "react";

import useSenderMode from "../hooks/UseSenderMode";
import { SessionStorageContext } from "../contexts/SessionStorageContext";

const isNotActive = (val: string): boolean => {
    return val === "testing" || val === "inactive";
};

const SenderModeBanner = (): ReactElement | null => {
    const session = useContext(SessionStorageContext);
    const status = useSenderMode(session.values.org, session.values.senderName);

    if (isNotActive(status)) {
        return <div>User is in testing mode</div>;
    }

    return null;
};

export default SenderModeBanner;
