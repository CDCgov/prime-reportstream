import { Faker } from "../utils/Faker";
import { createDbMock } from "../utils/MSWData";

import { createSettingsModels } from "./SettingsMock";

export function createModels(faker: Faker) {
    return {
        ...createSettingsModels(faker),
    };
}
export function createRSMock() {
    return createDbMock(createModels);
}
