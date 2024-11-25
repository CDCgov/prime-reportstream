/* eslint-env node */
/* eslint-disable no-undef */
/* eslint-disable no-console */
/* eslint-disable @typescript-eslint/require-await */

module.exports = async (page) => {
    page.on("request", (request) => {
        console.log(`Request: ${request.method()} ${request.url()}`);
    });

    page.on("response", (response) => {
        console.log(`Response: ${response.status()} ${response.url()}`);
    });
};
