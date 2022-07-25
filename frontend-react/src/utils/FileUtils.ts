// values taken from Report.kt
export const REPORT_MAX_ITEMS = 10000;
export const REPORT_MAX_ITEM_COLUMNS = 2000;
export const PAYLOAD_MAX_BYTES = 50 * 1000 * 1000; // no idea why this isn't in "k" (* 1024).
export const PAYLOAD_MAX_KBYTES = (PAYLOAD_MAX_BYTES / 1024).toLocaleString(
    "en-US",
    { maximumFractionDigits: 2, minimumFractionDigits: 2 }
);

export const parseCsvForError = (
    fileName: string,
    filecontent: string
): string | undefined => {
    // count the number of lines
    const linecount = (filecontent.match(/\n/g) || []).length + 1;
    if (linecount > REPORT_MAX_ITEMS) {
        return `The file '${fileName}' has too many rows. The maximum number of rows allowed is ${REPORT_MAX_ITEMS}.`;
    }
    if (linecount <= 1) {
        return `The file '${fileName}' doesn't contain any valid data. File should have a header line and at least one line of data.`;
    }

    // get the first line and examine it
    const firstline = (filecontent.match(/^(.*)\n/) || [""])[0];
    // ideally, the columns would be comma seperated, but they may be tabs, because the first
    // line is a header, we don't have to worry about escaped delimiters in strings (e.g. ,"Smith, John",)
    const columncount =
        (firstline.match(/,/g) || []).length ||
        (firstline.match(/\t/g) || []).length;

    if (columncount > REPORT_MAX_ITEM_COLUMNS) {
        return `The file '${fileName}' has too many columns. The maximum number of allowed columns is ${REPORT_MAX_ITEM_COLUMNS}.`;
    }
    // todo: this is a good place to do basic validation of the upload file. e.g. does it have
    // all the required columns? Are any rows obviously not correct (empty or obviously wrong type)?
};
