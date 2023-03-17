import {
    PAYLOAD_MAX_BYTES,
    PAYLOAD_MAX_KBYTES,
} from "../../../utils/FileUtils";
import { Format } from "../../../utils/TemporarySettingsAPITypes";

export enum ManagePublicKeyAction {
    SELECT_SENDER = "selectSender",
    SELECT_PUBLIC_KEY = "selectPublicKey",
    SAVE_PUBLIC_KEY = "savePublicKey",
    PUBLIC_KEY_SAVED = "publicKeySaved",
    PUBLIC_KEY_NOT_SAVED = "publicKeyNotSaved",
}

export function validateFileSelectedState(file: File) {
    try {
        // look at the filename extension.
        const fileNameArray = file.name.split(".");
        const uploadFileExtension = fileNameArray[fileNameArray.length - 1];

        if (uploadFileExtension.toUpperCase() !== Format.PEM) {
            return {
                fileError: "The file type must be of type .pem",
            };
        }

        if (file.size > PAYLOAD_MAX_BYTES) {
            return {
                fileError: `The file '${file.name}' is too large. The maximum file size is ${PAYLOAD_MAX_KBYTES}k`,
            };
        }

        return {
            fileName: file.name,
        };
    } catch (err: any) {
        console.warn(err);
        return {
            fileError: `An unexpected error happened: '${err.toString()}'`,
        };
    }
}
