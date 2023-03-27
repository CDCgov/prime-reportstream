import { SetupServer, setupServer } from "msw/node";

import { RSOrganizationSettings } from "../config/endpoints/settings";
import {
    ParentResolverMap,
    RelativePathMap,
    createDbMock,
} from "../utils/MSWData";

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

    describe("enhanced rest handler", () => {
        let mockServer: SetupServer | undefined;
        const parentResolverCallback = jest.fn();
        function parentResolver(req: any, res: any, ctx: any) {
            parentResolverCallback();
            return ctx.proxy.res;
        }
        const restHandlerScenarios: {
            label: string;
            args: [
                string,
                RelativePathMap | undefined,
                ParentResolverMap | undefined
            ];
        }[] = [
            {
                label: "default",
                args: ["https://localhost", undefined, undefined],
            },
            {
                label: "relativePathMap",
                args: [
                    "https://localhost",
                    { getList: "/settings/organizations" },
                    undefined,
                ],
            },
            {
                label: "parentResolver",
                args: [
                    "https://localhost",
                    undefined,
                    { getList: parentResolver },
                ],
            },
            {
                label: "relativePathMap and parentResolver",
                args: [
                    "https://localhost",
                    { getList: "/settings/organizations" },
                    { getList: parentResolver },
                ],
            },
        ];

        // In case a test errors, cleanup before next one
        afterEach(() => {
            mockServer?.close();
            mockServer &&= undefined;
        });

        test.each(restHandlerScenarios)("$label", async ({ args }) => {
            const [basePath, relativePathMap] = args;
            const getListPath = `${basePath}${
                relativePathMap?.getList ?? "/organizationSettings"
            }`;
            db.organizationSettings.createMany(10);
            const records = db.organizationSettings.getAllJson();
            const handlers = db.organizationSettings.toEnhancedRestHandlers(
                ...args
            );
            mockServer = setupServer(...handlers);
            mockServer.listen();
            const res = await window.fetch(getListPath);
            const data: RSOrganizationSettings[] = await res.json();
            expect(data).toStrictEqual(records);
        });
    });
});
