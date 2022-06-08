import {
    getStoredOktaToken,
    getStoredOrg,
} from "../contexts/SessionStorageTools";

import { ApiConfig } from "./api/Api";

// export const primeApiConfig = new ApiConfig({
//     root: `${process.env.REACT_APP_BACKEND_URL}/api`,
//     headers: {
//         Authorization: `Bearer ${getStoredOktaToken() || ""}`,
//         Organization: getStoredOrg() || "",
//     },
// });
