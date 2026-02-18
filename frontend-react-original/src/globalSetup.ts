import process from "node:process";

/**
 * Global setup for tests. You can fill in special-case env vars here.
 * DO NOT USE THIS IF YOU CAN USE A .ENV FILE INSTEAD FOR ENV VARS.
 */
function setup() {
    process.env.TZ = "UTC";
}

export default setup;
