import { useSuspenseQuery } from "@tanstack/react-query";
import { useCallback } from "react";
import useSessionContext from "../contexts/Session/useSessionContext";
import { DefaultService } from "../utils/Api/services.gen"

function UseSettingsOrganizations(){
    const { user } = useSessionContext();
    const memoizedDataFetch = useCallback(async () => {
        if (!user.isUserAdmin) {
            throw new Error("Permission denied")
        }
        
        const res = await DefaultService.getSettingsOrganizationss();

        switch(true) {
            case "error" in res: throw new Error("Error returned" + res.error);
            case "length" in res: return res;            
            default: throw new Error("Unknown response");
        }
    }, [user.isUserAdmin]);

    return useSuspenseQuery({
        queryKey: ["getSettingsOrganizations", user],
        queryFn: memoizedDataFetch,
    });

}

export default UseSettingsOrganizations