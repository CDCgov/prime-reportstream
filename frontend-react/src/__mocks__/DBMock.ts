import { Faker } from "../utils/Faker";
import { createDbMock } from "../utils/MSWData/CreateDbMock";

import { createSettingsModels } from "./SettingsMock";

export function createModels(faker: Faker) {
    return {
        ...createSettingsModels(faker),
    };
}

/**
 * Any test module could be making changes for db scenarios at any time
 * so we do not provide a singleton; only a factory function to create
 * a db within their environment. Model creation and rs db creation functions
 * provided seperately for flexibility.
 */
export function createRSMock() {
    return createDbMock(createModels);
}
