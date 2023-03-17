import { createDbMock } from "../utils/MSWData";

import { createSettingsModels } from "./SettingsMock";

//let { db, reset } = createDbMock([createSettingsModels]);
const test = createDbMock([createSettingsModels, { test: String }]);

describe("DBMock", () => {
    beforeEach(() => {
        reset();
    });

    describe("createMany", () => {
        test("multiple no initial", () => {
            db.organizationSettings.createMany(2);
            expect(db.organizationSettings.count()).toStrictEqual(2);
        });

        test("multiple all initial", () => {
            db.organizationSettings.createMany(2, [
                {
                    asdf: "",
                },
            ]);
            expect(db.organizationSettings.count()).toStrictEqual(2);
        });

        test("multiple some initial", () => {
            db.organizationSettings.createMany(2, [{}]);
            expect(db.organizationSettings.count()).toStrictEqual(2);
        });
    });

    test("Random record creation", () => {
        db.organizationSettings.createMany(2);
        expect(db.organizationSettings.count()).toStrictEqual(2);
        const records = db.organizationSettings.getAll();
        expect(records[0]).not.toStrictEqual(records[1]);
    });

    test("Predictable random record creation", () => {
        db.organizationSettings.createMany(2);
        const records = db.organizationSettings.getAll();
        reset();
        db.organizationSettings.createMany(2);
        expect(db.organizationSettings.getAll()).toStrictEqual(records);
    });
});
