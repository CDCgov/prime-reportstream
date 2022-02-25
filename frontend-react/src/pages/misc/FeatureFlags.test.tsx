import { _exportForTesting, CheckFeatureFlag } from "./FeatureFlags";

test("FeatureFlags basics", () => {
    const randomNewFeatureFlag = `testflag-${new Date().getTime()}`;
    // make sure random feature tests false
    expect(CheckFeatureFlag(randomNewFeatureFlag)).toBe(false);

    // add random feature and expect it to be true
    _exportForTesting.addFeatureFlag(randomNewFeatureFlag);
    expect(CheckFeatureFlag(randomNewFeatureFlag)).toBe(true);

    const randomNewFeatureFlag2 = `a${randomNewFeatureFlag}`; // a is before
    const randomNewFeatureFlag3 = `z${randomNewFeatureFlag}`; // z is after

    // add more flags and recheck (before and after)
    _exportForTesting.addFeatureFlag(randomNewFeatureFlag2);
    _exportForTesting.addFeatureFlag(randomNewFeatureFlag3);
    expect(CheckFeatureFlag(randomNewFeatureFlag)).toBe(true);

    // remove it and expect it to be false
    _exportForTesting.removeFeatureFlag(randomNewFeatureFlag);
    expect(CheckFeatureFlag(randomNewFeatureFlag)).toBe(false);

    // expect the other flags to still be true
    expect(CheckFeatureFlag(randomNewFeatureFlag2)).toBe(true);
    expect(CheckFeatureFlag(randomNewFeatureFlag3)).toBe(true);

    // test adding duplicate
    const startLen = _exportForTesting.getSavedFeatureFlags().length;
    _exportForTesting.addFeatureFlag(randomNewFeatureFlag3);
    const dupInsertLen = _exportForTesting.getSavedFeatureFlags().length;
    expect(dupInsertLen).toBe(startLen);
});
