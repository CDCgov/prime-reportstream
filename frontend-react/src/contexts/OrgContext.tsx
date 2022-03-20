import React, {
    createContext,
    PropsWithChildren,
    useState,
} from "react";

import { useNetwork } from "../network/hooks/useNetwork";
import { Organization, OrgApi } from "../network/api/OrgApi";
import {dummyOrg, dummyPayload} from "./OrgContext.test";

interface IOrgValues {
    org?: Organization;
    oktaGroup?: string;
}

interface IOrgController {
    updateOktaOrg: (val: string) => void;
}

export interface IOrgContext {
    values: IOrgValues;
    controller: IOrgController;
}

export const OrgContext = createContext<IOrgContext>({
    values: { },
    controller: {
        updateOktaOrg: (val: string) => {
            console.log("to please SonarCloud");
        },
    },
});

const OrgProvider: React.FC<any> = (props: PropsWithChildren<any>) => {
    const [oktaGroup, setOktaGroup] = useState<string>("ignore");
    const orgResponse = useNetwork<Organization>(OrgApi.detail(oktaGroup));

    const updateOktaOrg = (val: string) => {
        setOktaGroup(val);
    };

    const providerPayload: IOrgContext = {
        values: {
            org: orgResponse.data,
            oktaGroup: oktaGroup,
        },
        controller: {
            updateOktaOrg: updateOktaOrg,
        },
    };

    return (
        <OrgContext.Provider value={providerPayload}>
            {props.children}
        </OrgContext.Provider>
    );
};

export default OrgProvider;
