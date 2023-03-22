import { createDbMock } from "../utils/MSWData";

import { createSettingsModels } from "./SettingsMock";

let { db, reset } = createDbMock((faker) => ({
    ...createSettingsModels(faker),
}));

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
                    name: "test1",
                },
                {
                    name: "test2",
                },
            ]);
            expect(db.organizationSettings.count()).toStrictEqual(2);
            const all = db.organizationSettings.getAll();
            expect(all[0].name).toStrictEqual("test1");
            expect(all[1].name).toStrictEqual("test2");
        });

        test("multiple some initial", () => {
            db.organizationSettings.createMany(2, [{ stateCode: "LA" }]);
            expect(db.organizationSettings.count()).toStrictEqual(2);
            const all = db.organizationSettings.getAll();
            expect(all[0].stateCode).toStrictEqual("LA");
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
