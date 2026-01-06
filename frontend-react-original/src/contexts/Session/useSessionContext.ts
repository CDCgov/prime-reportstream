import { useContext } from "react";
import { SessionContext } from "./SessionProvider";

function useSessionContext() {
    return useContext(SessionContext);
}

export default useSessionContext;
