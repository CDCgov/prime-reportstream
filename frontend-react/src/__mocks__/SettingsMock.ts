import { primaryKey } from "@mswjs/data";

import {
    RSOrganizationSettings,
    RSService,
} from "../config/endpoints/settings";
import { Faker } from "../utils/Faker";
import { DBFactoryModel, undefinable } from "../utils/MSWData";

export function createSettingsModels(faker: Faker) {
    const settingsBaseModel: DBFactoryModel<RSService> = {
        name: primaryKey(() => faker.company.name()),
        organizationName: () => faker.company.name(),
        customerStatus: undefinable(() => faker.helpers.maybe(faker.word.verb)),
        topic: undefinable(() => faker.helpers.maybe(faker.word.noun)),
    };

    const organizationSettings: DBFactoryModel<RSOrganizationSettings> = {
        ...settingsBaseModel,
        createdAt: () =>
            faker.date.past(undefined, faker.defaultRefDate()).toISOString(),
        createdBy: () => faker.internet.userName(),
        description: () => faker.lorem.sentence(),
        filters: () => faker.helpers.multiple(faker.lorem.word),
        jurisdiction: () => faker.lorem.word(),
        version: () => faker.datatype.number(100),
        countyName: () => faker.address.county(),
        stateCode: () => faker.address.stateAbbr(),
    };

    return {
        organizationSettings,
    };
}
