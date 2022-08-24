import { useEffect, useState } from "react";

import { MetaTaggedResource } from "../../resources/OrgSettingsBaseResource";
import { formatDate } from "../../utils/misc";

type DisplayMetaProps = {
    metaObj?: MetaTaggedResource;
};

export const DisplayMeta = ({ metaObj }: DisplayMetaProps) => {
    const [metaData, setMetaData] = useState<MetaTaggedResource>();

    useEffect(() => {
        if (!metaObj) {
            return;
        }
        setMetaData(metaObj);
    }, [metaObj]);

    // if there is no object with data to display
    // we can return early. If the metadata object is present but
    // version, createdAt, and/or createdBy are missing
    // that case is handled below
    if (!metaData) {
        return null;
    }

    const { version, createdAt, createdBy } = metaData;

    // handle cases where individual metadata are not available
    const versionDisplay = version || version === 0 ? `v${version} ` : "";
    const createdAtDisplay = createdAt
        ? `[${formatDate(metaData.createdAt)}] `
        : "";
    const createdByDisplay = createdBy ?? "";

    return <>{`${versionDisplay}${createdAtDisplay}${createdByDisplay}`}</>;
};
