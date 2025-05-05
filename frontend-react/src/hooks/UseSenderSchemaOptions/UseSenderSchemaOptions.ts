import { FileType } from "../../utils/TemporarySettingsAPITypes";
import useOrganizationSender from "../api/organizations/UseOrganizationSender/UseOrganizationSender";

export enum StandardSchema {
    CSV = "upload-covid-19",
    CSV_OTC = "csv-otc-covid-19",
    HL7 = "hl7/hl7-ingest-covid-19-prod",
}

export interface SchemaOption {
    value: string;
    format: FileType;
    title: string;
}

export const STANDARD_SCHEMA_OPTIONS: SchemaOption[] = [
    {
        value: StandardSchema.CSV,
        format: FileType.CSV,
        title: `${StandardSchema.CSV} (${FileType.CSV})`,
    },
    {
        value: StandardSchema.CSV_OTC,
        format: FileType.CSV,
        title: `${StandardSchema.CSV_OTC} (${FileType.CSV})`,
    },
    {
        value: StandardSchema.HL7,
        format: FileType.HL7,
        title: `${StandardSchema.HL7} (${FileType.HL7})`,
    },
];

export type UseSenderSchemaOptionsHookResult = ReturnType<typeof useSenderSchemaOptions>;

export default function useSenderSchemaOptions() {
    const { data: senderDetail, isLoading, ...query } = useOrganizationSender({} as any);

    const schemaOptions =
        senderDetail && !(Object.values(StandardSchema) as string[]).includes(senderDetail.schemaName)
            ? ([
                  {
                      value: senderDetail.schemaName,
                      format: senderDetail.format,
                      title: `${senderDetail.schemaName} (${senderDetail.format})`,
                  },
                  ...STANDARD_SCHEMA_OPTIONS,
              ] as SchemaOption[])
            : STANDARD_SCHEMA_OPTIONS;

    return {
        ...query,
        isLoading,
        data: schemaOptions,
    };
}
