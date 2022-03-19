import React, {
    createContext,
    PropsWithChildren,
    useEffect,
    useState,
} from "react";

import { useNetwork } from "../network/hooks/useNetwork";
import { Organization, OrgApi } from "../network/api/OrgApi";

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
    values: {},
    controller: {
        updateOktaOrg: (val: string) => {
            console.log("to please SonarCloud");
        },
    },
});

const OrgProvider: React.FC<any> = (props: PropsWithChildren<any>) => {
    const [org, setOrg] = useState<Organization>();
    const [oktaGroup, setOktaGroup] = useState<string>("ignore");
    const orgResponse = useNetwork<Organization>(OrgApi.detail(oktaGroup));

    useEffect(() => {
        setOrg(orgResponse.data);
    }, [orgResponse.data]);

    const updateOktaOrg = (val: string) => {
        setOktaGroup(val);
    };

    const providerPayload: IOrgContext = {
        values: {
            org: org,
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
