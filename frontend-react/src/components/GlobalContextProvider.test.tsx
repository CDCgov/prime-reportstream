import {
    clearGlobalContext,
    getStoredOrg,
    setStoredOrg,
} from "./GlobalContextProvider";

it("Setting/getting/clearing sessionStorage works", () => {
    const testvalue = Number(Date.now()).toString();
    clearGlobalContext();
    expect(getStoredOrg()).toBeUndefined();

    setStoredOrg(testvalue);
    expect(getStoredOrg()).toBe(testvalue);
});
