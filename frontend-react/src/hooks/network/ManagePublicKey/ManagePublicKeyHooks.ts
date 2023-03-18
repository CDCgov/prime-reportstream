import {
    PAYLOAD_MAX_BYTES,
    PAYLOAD_MAX_KBYTES,
} from "../../../utils/FileUtils";

export const CONTENT_TYPE = "application/x-x509-ca-cert";
export const FORMAT = "PEM";

export enum ManagePublicKeyAction {
    SELECT_SENDER = "selectSender",
    SELECT_PUBLIC_KEY = "selectPublicKey",
    SAVE_PUBLIC_KEY = "savePublicKey",
    PUBLIC_KEY_SAVED = "publicKeySaved",
    PUBLIC_KEY_NOT_SAVED = "publicKeyNotSaved",
}

// Can this be moved and reused???
export function validateFileType(file: File, fileType: string) {
    try {
        // look at the filename extension.
        const fileNameArray = file.name.split(".");
        const uploadFileExtension = fileNameArray[fileNameArray.length - 1];

        if (uploadFileExtension.toUpperCase() !== fileType) {
            return {
                fileTypeError: `The file type must be of type .'${fileType.toLowerCase()}'`,
            };
        }

        if (file.size > PAYLOAD_MAX_BYTES) {
            return {
                fileTypeError: `The file '${file.name}' is too large. The maximum file size is ${PAYLOAD_MAX_KBYTES}k`,
            };
        }

        return {
            fileName: file.name,
        };
    } catch (err: any) {
        console.warn(err);
        return {
            fileTypeError: `An unexpected error happened: '${err.toString()}'`,
        };
    }
}

export function validateFileSize(file: File) {
    try {
        if (file.size > PAYLOAD_MAX_BYTES) {
            return {
                fileSizeError: `The file '${file.name}' is too large. The maximum file size is ${PAYLOAD_MAX_KBYTES}k`,
            };
        }

        return {
            fileName: file.name,
        };
    } catch (err: any) {
        console.warn(err);
        return {
            fileSizeError: `An unexpected error happened: '${err.toString()}'`,
        };
    }
}
