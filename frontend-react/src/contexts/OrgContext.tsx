import React, {
    createContext,
    PropsWithChildren,
    useEffect,
    useState,
} from "react";

import { useEndpoint } from "../network/hooks/UseEndpoint";
import { Organization, orgApi } from "../network/api/OrgApi";

import { GLOBAL_STORAGE_KEYS } from "./SessionStorageTools";

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
    const [oktaGroup, setOktaGroup] = useState<string>("ignore");
    const { call, response } = useEndpoint<Organization>(
        orgApi.getOrgDetail(oktaGroup)
    );

    const updateOktaOrg = (val: string) => {
        setOktaGroup(val);
    };

    /* If the endpoint parameter changes in any way, call() changes, and
     * triggers it through this effect. This will keep `response` up-to-date. */
    useEffect(() => {
        call();
    }, [call]);

    useEffect(() => {
        sessionStorage.setItem(GLOBAL_STORAGE_KEYS.GLOBAL_ORG, oktaGroup);
    }, [oktaGroup]);

    const providerPayload: IOrgContext = {
        values: {
            org: response.data,
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
