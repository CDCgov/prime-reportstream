import { useEffect, useState } from "react";

import { MetaData } from "../../resources/OrgSettingsBaseResource";

type Props = {
    metaJaon?: string;
    metaObj?: MetaData | undefined;
};

export const DisplayMeta = (props: Props) => {
    const [metaData, setMetaData] = useState<MetaData>();

    useEffect(() => {
        try {
            if (props.metaJaon === null && props.metaObj == null) {
                return;
            }
            const parsed = props.metaJaon
                ? JSON.parse(props.metaJaon)
                : props.metaObj;

            setMetaData(parsed);
        } catch (err: any) {
            console.error(err);
        }
    }, [props]);

    const formatDate = (date: string) => {
        try {
            // 'Thu, 3/31/2022, 4:50 AM'
            return new Intl.DateTimeFormat("en-US", {
                weekday: "short",
                year: "numeric",
                month: "numeric",
                day: "numeric",
                hour: "numeric",
                minute: "numeric",
            }).format(new Date(date));
        } catch (err: any) {
            console.error(err);
            return date;
        }
    };

    return metaData ? (
        <>{`v${metaData.version} ${formatDate(metaData.createdAt)} ${
            metaData.createdBy
        }`}</>
    ) : (
        <></>
    );
};
