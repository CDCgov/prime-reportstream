import { GovBanner, Header } from "@trussworks/react-uswds";
import classnames from "classnames";
import React, { Suspense } from "react";

import SenderModeBanner from "../SenderModeBanner/SenderModeBanner";
import Spinner from "../../components/Spinner";
import { ReportStreamNavbar } from "../ReportStreamNavbar/ReportStreamNavbar";

import styles from "./ReportStreamHeader.module.scss";

export interface ReportStreamHeaderProps extends React.PropsWithChildren {
    blueVariant?: boolean;
    isNavHidden?: boolean;
}

const suspenseFallback = <Spinner size={"fullpage"} />;

export const ReportStreamHeaderBase = ({
    blueVariant,
    children,
    isNavHidden,
}: ReportStreamHeaderProps) => {
    return (
        <header>
            <GovBanner aria-label="Official government website" />
            {!isNavHidden && <SenderModeBanner />}
            <Header
                basic={true}
                className={classnames(styles.Navbar, {
                    [styles.NavbarBlueVariant]: blueVariant,
                    [styles.NavbarDefault]: !blueVariant,
                })}
            >
                {!isNavHidden && children}
            </Header>
        </header>
    );
};

export function ReportStreamHeader(props: ReportStreamHeaderProps) {
    return (
        <ReportStreamHeaderBase {...props}>
            <Suspense fallback={suspenseFallback}>
                <ReportStreamNavbar />
            </Suspense>
        </ReportStreamHeaderBase>
    );
}

export default ReportStreamHeader;
