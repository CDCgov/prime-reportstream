import { API } from "./NewApi";
import { WatersResponse } from "./WatersApi";

const ValidateApi: API = new API(WatersResponse, "/api").addEndpoint(
    "validate",
    "/validate",
    ["POST"]
);

export default ValidateApi;
