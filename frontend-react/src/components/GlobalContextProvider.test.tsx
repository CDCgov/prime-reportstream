import { clearGlobalContext, getStoredOrg, setStoredOrg, useGlobalContext } from "./GlobalContextProvider"

// const store = mockStore({ user: { isAdmin: false } });

it("Setting/getting/clearing sessionStorage works", () => {
    const testvalue = Number(Date.now()).toString();
    clearGlobalContext();
    expect(getStoredOrg()).toBeUndefined();

    setStoredOrg(testvalue);
    expect(getStoredOrg()).toBe(testvalue);
});
