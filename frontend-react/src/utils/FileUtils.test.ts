import {
    parseCsvForError,
    REPORT_MAX_ITEMS,
    REPORT_MAX_ITEM_COLUMNS,
} from "./FileUtils";

describe("parseCsvForError", () => {
    test("returns expected error string if max number of lines is exceeded", () => {
        const fakeFileContent = Array(REPORT_MAX_ITEMS + 2)
            .fill("\n")
            .join();

        expect(parseCsvForError("fakeFile", fakeFileContent)).toEqual(
            `The file 'fakeFile' has too many rows. The maximum number of rows allowed is ${REPORT_MAX_ITEMS}.`,
        );
    });

    test("returns expected error string if no lines present", () => {
        const fakeFileContent = "";

        expect(parseCsvForError("fakeFile", fakeFileContent)).toEqual(
            "The file 'fakeFile' doesn't contain any valid data. File should have a header line and at least one line of data.",
        );
    });

    test("returns expected error string if max number of lines is exceeded", () => {
        const fakeFileContent = Array(REPORT_MAX_ITEM_COLUMNS + 1)
            .fill(",")
            .concat("\n")
            .join();

        expect(parseCsvForError("fakeFile", fakeFileContent)).toEqual(
            `The file 'fakeFile' has too many columns. The maximum number of allowed columns is ${REPORT_MAX_ITEM_COLUMNS}.`,
        );
    });
});
