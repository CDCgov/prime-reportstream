import { SetupServer, setupServer } from "msw/node";

import { RSOrganizationSettings } from "../config/endpoints/settings";
import { createDbMock } from "../utils/MSWData/CreateDbMock";
import {
    RelativePathMap,
    ParentResolverMap,
    factoryRestHandlerTupleToMap,
    RestHandlerMethods,
} from "../utils/MSWData/EnhancedFactory";

import { createModels } from "./DBMock";

let { db, reset } = createDbMock(createModels);

describe("DBMock", () => {
    beforeEach(() => {
        reset();
    });

    describe("createMany", () => {
        const createManyScenarios: {
            label: string;
            args: [number, Partial<RSOrganizationSettings>[] | undefined];
        }[] = [
            {
                label: "predictably random",
                args: [2, undefined],
            },
            {
                label: "all with initial values",
                args: [2, [{ name: "test1" }, { name: "test2" }]],
            },
            {
                label: "some with initial values",
                args: [2, [{ stateCode: "LA" }]],
            },
        ];

        test.each(createManyScenarios)("$label", ({ args }) => {
            const [num, initial = []] = args;
            db.organizationSettings.createMany(...args);
            expect(db.organizationSettings.count()).toStrictEqual(num);
            const all = db.organizationSettings.getAll();
            for (const [i, init] of initial.entries()) {
                for (const [k, v] of Object.entries(init)) {
                    // check initialized value
                    expect(
                        all[i][k as keyof (typeof all)[number]]
                    ).toStrictEqual(v);
                }
            }
            if (initial.length === 0) {
                // check is random
                expect(all[0]).not.toStrictEqual(all[1]);
                reset();
                db.organizationSettings.createMany(2);
                // check is predictably random
                expect(db.organizationSettings.getAll()).toStrictEqual(all);
            }
        });
    });

    describe("json serialized return methods", () => {
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
                label: "default getList",
                args: ["https://localhost", undefined, undefined],
            },
            {
                label: "relativePathMap getList",
                args: [
                    "https://localhost",
                    { getList: "/settings/organizations" },
                    undefined,
                ],
            },
            {
                label: "relativePathMap get by primary key",
                args: [
                    "https://localhost",
                    { get: "/settings/organizations/:name" },
                    undefined,
                ],
            },
            {
                label: "relativePathMap post",
                args: [
                    "https://localhost",
                    { post: "/settings/organizations" },
                    undefined,
                ],
            },
            {
                label: "parentResolver getList",
                args: [
                    "https://localhost",
                    undefined,
                    { getList: parentResolver },
                ],
            },
            {
                label: "parentResolver get by primary key",
                args: ["https://localhost", undefined, { get: parentResolver }],
            },
            {
                label: "relativePathMap and parentResolver getList",
                args: [
                    "https://localhost",
                    { getList: "/settings/organizations" },
                    { getList: parentResolver },
                ],
            },
            {
                label: "relativePathMap and parentResolver by primary key",
                args: [
                    "https://localhost",
                    { get: "/settings/organizations/:name" },
                    { get: parentResolver },
                ],
            },
        ];

        // In case a test errors, cleanup before next one
        afterEach(() => {
            mockServer?.close();
            mockServer &&= undefined;
        });

        test.each(restHandlerScenarios)("$label", async ({ args }) => {
            const [basePath, relativePathMap, parentResolverMap] = args;
            db.organizationSettings.createMany(10);

            const handlers = db.organizationSettings.toEnhancedRestHandlers(
                ...args
            );
            const handlerMap = factoryRestHandlerTupleToMap(handlers);
            mockServer = setupServer(...handlers);
            mockServer.listen();

            const records = db.organizationSettings.getAllJson();
            const methods = Object.keys(relativePathMap ?? parentResolverMap ?? {}) as RestHandlerMethods[];
            // If no custom paths/resolvers given, we're just testing a default handler
            if (methods.length === 0) {
                methods.push("getList");
            }

            for (const method of methods) {
                const relativePath = relativePathMap?.[method];
                const path =
                    relativePath ? `${basePath}${relativePath}` : handlerMap[method]?.info.path.toString();

                // check is handler data successful
                if (method === "getList") {
                    // Get list of records via getList (entity index page)
                    const res = await window.fetch(path);
                    const data: RSOrganizationSettings[] = await res.json();
                    expect(data).toStrictEqual(records);
                } else if(method === "get") {
                    // Find an arbitrary record via get
                    const key = path.match(/:(\w+)/)![1];
                    const primaryKey =
                        records[0][key as keyof RSOrganizationSettings]!.toString();
                    const record = db.organizationSettings.findFirstJson({
                        where: {
                            [key]: {
                                equals: primaryKey,
                            },
                        },
                    });
                    const pkUrl = path.replace(`:${key}`, primaryKey);
                    const res = await window.fetch(pkUrl);
                    const data: RSOrganizationSettings = await res.json();
                    expect(data).toStrictEqual(record);
                } else if(method === "post") {
                    // Take an arbitrary record, remove from db, readd via post, and check db for record
                    const newRecord = records[4];
                    db.organizationSettings.delete({where: {name: {equals: newRecord.name}}});
                    await window.fetch(path, {method: "POST", body: JSON.stringify(newRecord), headers: {"Content-Type": "application/json"}});
                    const record = db.organizationSettings.findFirstJson({where: {name: {equals: newRecord.name}}});
                    expect(record).toStrictEqual(newRecord);
                }
            }

            if (parentResolverMap) {
                // check is handler parent resolver successful
                expect(parentResolverCallback).toBeCalled();
            }
        });
    });
});
