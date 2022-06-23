import { useEffect, useState } from "react";

import { MetaData } from "../../resources/OrgSettingsBaseResource";
import { formatDate } from "../../utils/misc";

type Props = {
    metaObj?: MetaData | undefined;
};

export const DisplayMeta = (props: Props) => {
    const [metaData, setMetaData] = useState<MetaData>();

    useEffect(() => {
        if (!props.metaObj) {
            return;
        }
        setMetaData(props.metaObj);
    }, [props]);

    return metaData ? (
        <>{`v${metaData.version} [${formatDate(metaData.createdAt)}] 
        ${metaData.createdBy}`}</>
    ) : null;
};
