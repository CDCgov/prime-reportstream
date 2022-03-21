/**
 * splitOn('foo', 1);
 * // ["f", "oo"]
 *
 * splitOn([1, 2, 3, 4], 2);
 * // [[1, 2], [3, 4]]
 *
 * splitOn('fooBAr', 1, 4);
 * //  ["f", "ooB", "Ar"]
 */
export const splitOn: {
    <T = string>(s: T, ...i: number[]): T[];
    <T extends any[]>(s: T, ...i: number[]): T[];
} = <T>(slicable: string | T[], ...indices: number[]) =>
    [0, ...indices].map((n, i, m) => slicable.slice(n, m[i + 1]));
