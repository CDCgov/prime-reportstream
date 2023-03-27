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

    describe("model createMany", () => {
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
            const [basePath, relativePathMap, parentResolverMap] = args;
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
            if (parentResolverMap) {
                expect(parentResolverCallback).toBeCalled();
            }
        });
    });
});
