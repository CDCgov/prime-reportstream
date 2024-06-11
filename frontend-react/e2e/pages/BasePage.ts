import { Locator, Page, Request, Route } from "@playwright/test";
import appInsightsConfig from "../mocks/appInsightsConfig.json" assert { type: "json" };
import { TestArgs } from "../test";

export type RouteHandlers = Record<string, Parameters<Page["route"]>[1]>;
export type MockRouteCache = Record<string, Response>;
export type GotoOptions = Parameters<Page["goto"]>[1];

export interface BasePageProps {
    url: string;
    title: string;
    heading?: Locator;
}

export type RouteFulfillOptions = Exclude<
    Parameters<Route["fulfill"]>[0],
    undefined
> & { isMock?: boolean };
export type RouteFulfillOptionsFn = (request: Request) => RouteFulfillOptions;
export type RouteHandlerFn = (route: Route, request: Request) => Promise<void>;
export type RouteHandlerFulfillOptions =
    | RouteFulfillOptions
    | RouteFulfillOptionsFn;
export type RouteHandlerEntry = [
    url: string,
    fulfillOptions: RouteHandlerFulfillOptions,
];

export interface GotoRouteHandlerOptions {
    mock?: RouteHandlers;
    handlers?: RouteHandlers;
    mockError?: boolean | RouteFulfillOptions;
}

export type BasePageTestArgs = TestArgs<"page" | "storageState">;

export abstract class BasePage {
    readonly page: Page;
    readonly url: string;
    readonly title: string;
    readonly testArgs: BasePageTestArgs;
    readonly routeHandlers: Map<string, Parameters<Page["route"]>[1]>;
    protected _mockError?: RouteHandlerFulfillOptions;
    protected _mockRouteCache: MockRouteCache;

    readonly heading: Locator;

    constructor(
        { url, title, heading }: BasePageProps,
        testArgs: BasePageTestArgs,
    ) {
        this.page = testArgs.page;
        this.url = url;
        this.title = title;
        this.testArgs = testArgs;
        this.routeHandlers = new Map();
        this._mockRouteCache = {};
        this.heading = heading ?? this.page.locator("INVALID");
    }

    get isMocked() {
        return !this.testArgs.isMockDisabled;
    }

    get mockError(): RouteHandlerFulfillOptions | undefined {
        return this._mockError;
    }

    set mockError(
        err: boolean | number | RouteHandlerFulfillOptions | undefined,
    ) {
        if (err == null || err === false) {
            this._mockError = undefined;
            return;
        }

        this._mockError =
            typeof err === "function" || typeof err === "object"
                ? err
                : {
                      status: typeof err === "number" ? err : 500,
                  };
    }

    get isErrorExpected() {
        return !!this.mockError;
    }

    async reload() {
        await this.route();

        return await this.page.reload();
    }

    async goto(opts?: GotoOptions) {
        await this.route();

        return await this.page.goto(this.url, {
            waitUntil: "domcontentloaded",
            ...opts,
        });
    }

    async route() {
        for (const [url, handler] of this.routeHandlers.entries()) {
            await this.page.route(url, handler);
        }
    }

    addRouteHandlers(items: RouteHandlerEntry[]) {
        for (const [url, _fulfillOptions] of items) {
            const handler: RouteHandlerFn = async (route, request) => {
                const { isMock, ...fulfillOptions } =
                    typeof _fulfillOptions === "function"
                        ? _fulfillOptions(request)
                        : _fulfillOptions;
                const mockErrorFulfillOptions = isMock
                    ? typeof this.mockError === "function"
                        ? this.mockError(request)
                        : this.mockError
                    : undefined;
                const mockCacheFulfillOptions = isMock
                    ? await this.getMockCacheFulfillOptions(url)
                    : undefined;
                const mockOverrideFulfillOptions =
                    mockErrorFulfillOptions ?? mockCacheFulfillOptions;

                if (isMock && !this.isMocked)
                    throw new Error("Mocks are disabled");

                return await route.fulfill(
                    mockOverrideFulfillOptions ?? fulfillOptions,
                );
            };
            this.routeHandlers.set(url, handler);
        }
    }

    addMockRouteHandlers(items: RouteHandlerEntry[]) {
        return this.addRouteHandlers(
            items.map(([url, _fulfillOptions]) => {
                if (typeof _fulfillOptions === "object") {
                    return [
                        url,
                        {
                            isMock: true,
                            ..._fulfillOptions,
                        },
                    ];
                }

                const fn: RouteFulfillOptionsFn = (request) => {
                    return {
                        isMock: true,
                        ..._fulfillOptions(request),
                    };
                };
                return [url, fn];
            }),
        );
    }

    addDefaultRouteHandlers() {
        return this.addRouteHandlers([
            // Azure Application Insights Tracking
            [
                "*.in.applicationinsights.azure.com/v2/track",
                (request) => {
                    const itemsCount = (request.postDataJSON() as any[]).length;
                    return {
                        json: {
                            itemsReceived: itemsCount,
                            itemsAccepted: itemsCount,
                            appId: null,
                            errors: [],
                        },
                    };
                },
            ],
            // Azure Application Insights Config
            [
                "https://js.monitor.azure.com/scripts/b/ai.config.*.cfg.json",
                {
                    json: appInsightsConfig,
                },
            ],
            // Google Analytics
            [
                "https://www.google-analytics.com/**",
                {
                    status: 204,
                },
            ],
        ]);
    }

    async getMockCacheFulfillOptions(url: string) {
        const cache = this._mockRouteCache[url];
        if (!cache) return cache;

        return {
            body: await cache.text(),
            headers: Object.fromEntries(cache.headers.entries()),
            path: cache.url,
            status: cache.status,
        };
    }
}
