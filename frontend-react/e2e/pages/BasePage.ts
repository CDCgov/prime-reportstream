import { Locator, Page } from "@playwright/test";
import { TestArgs } from "../test";

export type RouteHandlers = Record<string, Parameters<Page['route']>[1]>
export type GotoOptions = Parameters<Page["goto"]>[1];

export interface BasePageProps {
    url: string
    title: string
    heading?: Locator
}

export interface GotoRouteHandlers {
    mock?: RouteHandlers
    handlers?: RouteHandlers
}

export abstract class BasePage {
    readonly page: Page;
    readonly url: string;
    readonly title: string;
    readonly testArgs: TestArgs;
    readonly routeHandlers: RouteHandlers;

    readonly heading: Locator;

    constructor({url, title, heading}: BasePageProps, testArgs: TestArgs) {
        this.page = testArgs.page;
        this.url = url;
        this.title = title;
        this.testArgs = testArgs;
        this.routeHandlers = {};
        this.heading = heading ?? this.page.locator("INVALID")
    }

    get isMocked() {
        return !this.testArgs.isMockDisabled
    }

    async goto(opts?: GotoOptions, {mock, handlers}: GotoRouteHandlers = {}) {
        if(mock && this.isMocked) {
            await this.mock(mock)
        }

        if(handlers) {
            await this.route(handlers);
        }

        return await this.page.goto(this.url, {
            waitUntil: "domcontentloaded",
            ...opts,
        });
    }

    async route(handlers: RouteHandlers) {
        for (const [url, handler] of Object.entries(handlers)) {
            await this.page.route(url, handler);
            this.routeHandlers[url] = handler;
        }
    }

    async mock(handlers: RouteHandlers) {
        if(this.isMocked) throw new Error("Mocking disabled");

        await this.route(handlers);
    }
}