import React, {
    createContext,
    PropsWithChildren,
    useMemo,
    useState,
} from "react";

import { useEndpoint } from "../network/hooks/UseEndpoint";
import { orgApi, Sender } from "../network/api/OrgApi";

import { getStoredOrg } from "./SessionStorageTools";

interface ISenderContext {
    sender?: Sender;
    update: (val: string) => void;
}

export const SenderContext = createContext<ISenderContext>({
    sender: {
        name: "",
        organizationName: "",
        format: "CSV",
        topic: "",
        customerStatus: "",
        schemaName: "",
    },
    update: (val: string) => {
        console.log("to please SonarCloud");
    },
});

const SenderProvider: React.FC<any> = (props: PropsWithChildren<any>) => {
    const [sender, setSender] = useState<string>("ignore");
    const response = useEndpoint<Sender>(
        orgApi.getSenderDetail(getStoredOrg(), sender)
    );

    const updateOktaOrg = (val: string) => {
        setSender(val);
    };

    const payload = useMemo((): ISenderContext => {
        return {
            sender: response.data,
            update: updateOktaOrg,
        };
    }, [response.data]);

    return (
        <SenderContext.Provider value={payload}>
            {props.children}
        </SenderContext.Provider>
    );
};

export default SenderProvider;
