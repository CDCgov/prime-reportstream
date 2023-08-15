import {
    ExportToCsv,
    Options as ExportToCsvOptions,
} from "export-to-csv-fix-source-map";

// values taken from Report.kt
export const REPORT_MAX_ITEMS = 10000;
export const REPORT_MAX_ITEM_COLUMNS = 2000;
export const PAYLOAD_MAX_BYTES = 50 * 1000 * 1000; // no idea why this isn't in "k" (* 1024).
export const PAYLOAD_MAX_KBYTES = (PAYLOAD_MAX_BYTES / 1024).toLocaleString(
    "en-US",
    { maximumFractionDigits: 2, minimumFractionDigits: 2 },
);

export const parseCsvForError = (
    fileName: string,
    fileContent: string,
): string | undefined => {
    // count the number of lines
    const lineCount = (fileContent.match(/\n/g) || []).length + 1;
    if (lineCount > REPORT_MAX_ITEMS) {
        return `The file '${fileName}' has too many rows. The maximum number of rows allowed is ${REPORT_MAX_ITEMS}.`;
    }
    if (lineCount <= 1) {
        return `The file '${fileName}' doesn't contain any valid data. File should have a header line and at least one line of data.`;
    }

    // get the first line and examine it
    const firstLine = (fileContent.match(/^(.*)\n/) || [""])[0];
    // ideally, the columns would be comma seperated, but they may be tabs, because the first
    // line is a header, we don't have to worry about escaped delimiters in strings (e.g. ,"Smith, John",)
    const columnCount =
        (firstLine.match(/,/g) || []).length ||
        (firstLine.match(/\t/g) || []).length;

    if (columnCount > REPORT_MAX_ITEM_COLUMNS) {
        return `The file '${fileName}' has too many columns. The maximum number of allowed columns is ${REPORT_MAX_ITEM_COLUMNS}.`;
    }
    // todo: this is a good place to do basic validation of the upload file. e.g. does it have
    // all the required columns? Are any rows obviously not correct (empty or obviously wrong type)?
};

// default options for exporting to CSV
const EXPORT_TO_CSV_DEFAULTS: ExportToCsvOptions = {
    decimalSeparator: ".",
    fieldSeparator: ",",
    filename: `Exported CSV`,
    quoteStrings: '"',
    showLabels: true,
    showTitle: false,
    title: `Exported CSV`,
    useBom: true,
    useKeysAsHeaders: true,
    useTextFile: false,
};

export function saveToCsv(
    data: any,
    options: Partial<ExportToCsvOptions> = {},
) {
    const finalOptions: ExportToCsvOptions = {
        ...EXPORT_TO_CSV_DEFAULTS,
        ...options,
    };

    const csvExporter = new ExportToCsv(finalOptions);
    csvExporter.generateCsv(data);
}

export const validateFileType = (
    file: File,
    fileExt: string,
    mimeType: string,
) => {
    // look at the filename extension.
    const fileNameArray = file.name.split(".");
    const uploadFileExtension = fileNameArray[fileNameArray.length - 1];

    if (uploadFileExtension.toUpperCase() !== fileExt) {
        return `The file extension must be of type .'${fileExt.toLowerCase()}'`;
    }

    if (file.type !== mimeType) {
        return `The file MIME type must be of type .'${mimeType}'`;
    }

    return;
};

export const validateFileSize = (file: File) => {
    if (file.size > PAYLOAD_MAX_BYTES) {
        return `The file '${file.name}' is too large. The maximum file size is ${PAYLOAD_MAX_KBYTES}k`;
    }

    return;
};
