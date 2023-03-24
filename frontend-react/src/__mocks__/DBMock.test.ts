import { setupServer } from "msw/node";

import { RSOrganizationSettings } from "../config/endpoints/settings";
import { createDbMock } from "../utils/MSWData";

import { createModels } from "./DBMock";

let { db, reset } = createDbMock(createModels);

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

    // TODO: use a scenario array of function names for json tests?
    test("getAllJson", () => {
        db.organizationSettings.createMany(2);
        const records = JSON.parse(
            JSON.stringify(db.organizationSettings.getAll())
        );
        expect(db.organizationSettings.getAllJson()).toStrictEqual(records);
    });

    test("findFirstJson", () => {
        db.organizationSettings.createMany(2);
        const record = db.organizationSettings.getAllJson()[0];
        const findRecord = db.organizationSettings.findFirstJson({
            where: { name: { equals: record.name } },
        });
        expect(record).toStrictEqual(findRecord);
    });

    test("enhanced rest handler default", async () => {
        db.organizationSettings.createMany(10);
        const records = db.organizationSettings.getAllJson();
        const handlers =
            db.organizationSettings.toEnhancedRestHandlers("https://localhost");
        const mockServer = setupServer(...handlers);
        mockServer.listen();
        const res = await window.fetch(
            "https://localhost/organizationSettings"
        );
        const data: RSOrganizationSettings[] = await res.json();
        mockServer.close();
        expect(data).toStrictEqual(records);
    });

    // TODO: use scenario array?
    test("enhanced rest handler relativePathMap", () => {});
    test("enhanced rest handler parentHandler", () => {});
    test("enhanced rest handler relativePathMap and parentHandler", () => {});
});
