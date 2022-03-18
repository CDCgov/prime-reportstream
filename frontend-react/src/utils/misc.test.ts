import { splitOn } from "./misc";

test("splitOn test", () => {
    const r1 = splitOn("foo", 1);
    expect(JSON.stringify(r1)).toBe(`["f","oo"]`);

    const r2 = splitOn([1, 2, 3, 4], 2);
    expect(JSON.stringify(r2)).toBe(`[[1,2],[3,4]]`);

    const r3 = splitOn("fooBAr", 1, 4);
    expect(JSON.stringify(r3)).toBe(`["f","ooB","Ar"]`);

    // boundary conditions
    const r4 = splitOn("fooBAr", 0, 6);
    expect(JSON.stringify(r4)).toBe(`["","fooBAr",""]`);
});
