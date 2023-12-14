import { PropsWithChildren } from "react";

import { formatDate } from "../../utils/misc";
import { RsSettingMeta } from "../../config/endpoints/settings";

export interface MetaDisplayProps extends PropsWithChildren, RsSettingMeta {}

export default function MetaDisplay({
    createdAt,
    createdBy,
    version,
    children,
}: MetaDisplayProps) {
    // handle cases where individual metadata are not available
    const versionDisplay = version || version === 0 ? `v${version} ` : "";
    const createdAtDisplay = createdAt ? `[${formatDate(createdAt)}] ` : "";
    const createdByDisplay = createdBy ?? "";

    return (
        <>
            {`${versionDisplay}${createdAtDisplay}${createdByDisplay}`}
            {children}
        </>
    );
}
