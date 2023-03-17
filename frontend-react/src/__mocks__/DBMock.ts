import { createDbMock } from "../utils/MSWData";

import { createSettingsModels } from "./SettingsMock";

export const db = createDbMock([createSettingsModels]);
