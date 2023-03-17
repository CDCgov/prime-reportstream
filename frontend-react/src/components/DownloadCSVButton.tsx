import { Button, Icon } from "@trussworks/react-uswds";
import { ExportToCsv } from "export-to-csv-fix-source-map";

import { ResponseError } from "../config/endpoints/waters";

type DownloadCSVButtonProps = {
    secondaryButton?: boolean;
    csvOptions: {
        fieldSeparator: string;
        quoteStrings: string;
        decimalSeparator: string;
        showLabels: boolean;
        showTitle: boolean;
        filename: string;
        title: string;
        useTextFile: boolean;
        useBom: boolean;
        useKeysAsHeaders: boolean;
    };
    data: ResponseError[];
    buttonText: string;
};

export const DownloadCSVButton = ({
    secondaryButton,
    csvOptions,
    data,
    buttonText,
}: DownloadCSVButtonProps) => {
    return (
        <Button
            className={`usa-button flex-align-self-start height-5 margin-top-4 ${
                secondaryButton && "usa-button--outline"
            }`}
            type={"button"}
            onClick={() => {
                const options = {
                    ...csvOptions,
                };
                const csvExporter = new ExportToCsv(options);
                csvExporter.generateCsv(data);
            }}
        >
            {buttonText} <Icon.FileDownload />
        </Button>
    );
};
