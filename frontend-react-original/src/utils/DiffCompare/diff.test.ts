import { Diff, SES_TYPE } from "./diff";

describe("text diff algorithm", () => {
    test("swap a single character", () => {
        const a = `abcdefghijklmnopqrstuvwxyz`;
        const b = `abcDefghijklmnopqrstuvwxyz`;

        const differ = Diff(a, b);
        differ.compose();
        const ses = differ.getses();
        expect(ses.length).toBe(4); // #1 matching text, #2 a diff, #3, b diff, #4 matching remaining

        expect(ses[0]?.sestype).toBe(SES_TYPE.COMMON);
        expect(ses[0]?.index).toBe(1);
        expect(ses[0]?.len).toBe(3);

        expect(ses[1]?.sestype).toBe(SES_TYPE.DELETE);
        expect(ses[1]?.index).toBe(4);
        expect(ses[1]?.len).toBe(1);

        expect(ses[2]?.sestype).toBe(SES_TYPE.ADD);
        expect(ses[2]?.index).toBe(4);
        expect(ses[2]?.len).toBe(1);

        expect(ses[3]?.sestype).toBe(SES_TYPE.COMMON);
        expect(ses[3]?.index).toBe(5);
        expect(ses[3]?.len).toBe(22);
    });

    test("remove a single character", () => {
        const a = `abcdefghijklmnopqrstuvwxyz`;
        const b = `abcefghijklmnopqrstuvwxyz`;

        const differ = Diff(a, b);
        differ.compose();
        const ses = differ.getses();

        expect(ses.length).toBe(3); // #1 matching text, #2 a diff, #3, b diff, #4 matching remaining
        expect(ses[0]?.sestype).toBe(SES_TYPE.COMMON);
        expect(ses[0]?.index).toBe(1);
        expect(ses[0]?.len).toBe(3);

        expect(ses[1]?.sestype).toBe(SES_TYPE.DELETE);
        expect(ses[1]?.index).toBe(4);
        expect(ses[1]?.len).toBe(1);

        expect(ses[2]?.sestype).toBe(SES_TYPE.COMMON);
        expect(ses[2]?.index).toBe(4);
        expect(ses[2]?.len).toBe(22);
    });
});
