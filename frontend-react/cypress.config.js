"use strict";
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
var _a;
Object.defineProperty(exports, "__esModule", { value: true });
var path_1 = require("path");
var dotenv_1 = require("dotenv");
var cypress_1 = require("cypress");
var _b = process.env.CYPRESS_ENV, cypressEnv = _b === void 0 ? "development" : _b;
// Populate Cypress env with values from .env.* file
dotenv_1.default.config({ path: path_1.default.resolve(process.cwd(), "./.env.".concat(cypressEnv)) });
var cypressConfig = (await (_a = path_1.default.resolve(process.cwd(), "./cypress/cypress.".concat(cypressEnv, ".json")), Promise.resolve().then(function () { return require(_a); }))).default;
function getEnvOrDefault(name, defaultValue) {
    if (!!cypressConfig[name]) {
        return cypressConfig[name];
    }
    if (defaultValue) {
        return defaultValue;
    }
    throw new Error("No value supplied for env variable ".concat(name));
}
var env = __assign(__assign({}, cypressConfig), { oktaClientId: getEnvOrDefault("oktaClientId", process.env.VITE_OKTA_CLIENTID), oktaSecret: getEnvOrDefault("oktaSecret", process.env.VITE_OKTA_SECRET), oktaUrl: getEnvOrDefault("oktaUrl", process.env.VITE_OKTA_URL), baseUrl: getEnvOrDefault("baseUrl", process.env.VITE_BASE_URL), basePageTitle: process.env.VITE_TITLE });
exports.default = (0, cypress_1.defineConfig)({
    e2e: {
        baseUrl: env.baseUrl,
        specPattern: "cypress/e2e/**/*.cy.(js|ts)",
        setupNodeEvents: function (on) {
            // configure plugins here
            on("task", {
                log: function (message) {
                    console.log("[RS LOG] ".concat(message));
                    return null;
                },
                getAuth: function () {
                    return global.auth || null;
                },
                setAuth: function (auth) {
                    global.auth = auth;
                    return null;
                },
            });
        },
    },
    env: env,
    viewportHeight: 768,
    viewportWidth: 1440,
});
