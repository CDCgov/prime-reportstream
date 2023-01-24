import { AdminSendFailure } from "hooks/api/Admin/UseAdminSendFailures";
import { formatDate } from "utils/misc";
import { AdminAction } from "hooks/api/Admin/UseAdminResends";
import { AdminReceiverConnectionStatus } from "hooks/api/Admin/UseAdminReceiversConnectionStatus";

type Transformers<T> = { [P in keyof T]?: (value: T[P]) => string };

export function createObjectSimpleFilterFn<T extends object>(
    keys: (keyof T)[],
    transformers: Transformers<T> = {}
) {
    return (obj: T, search: string) => {
        if (!search) {
            // no search returns EVERYTHING
            return true;
        }

        const fullStr = keys
            .reduce((a, k) => {
                const value = obj[k];
                const transformer = transformers[k];
                a.push(value);
                if (transformer) {
                    a.push(transformer(value));
                }
                return a;
            }, [] as unknown[])
            .join(" ")
            .toLocaleLowerCase();
        return fullStr.includes(search.toLocaleLowerCase());
    };
}

export const adminActionFilterMatch = createObjectSimpleFilterFn<AdminAction>([
    "actionParams",
    "actionResponse",
    "actionResult",
]);

export const adminSendFailureFilterMatch =
    createObjectSimpleFilterFn<AdminSendFailure>(
        [
            "reportId",
            "receiver",
            "fileName",
            "actionResult",
            "bodyUrl",
            "failedAt",
        ],
        { failedAt: formatDate }
    );

export const adminReceiverStatusFilterOnName =
    createObjectSimpleFilterFn<AdminReceiverConnectionStatus>([
        "organizationName",
        "receiverName",
    ]);

export const adminReceiverStatusFilterOnCheckResultStr =
    createObjectSimpleFilterFn<AdminReceiverConnectionStatus>([
        "connectionCheckResult",
    ]);
