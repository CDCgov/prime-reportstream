import { GovBanner, Header, Title } from "@trussworks/react-uswds";
import classnames from "classnames";
import { PropsWithChildren, ReactElement } from "react";

import styles from "./ReportStreamHeader.module.scss";
import useSessionContext from "../../contexts/Session/useSessionContext";
import { USLink } from "../USLink";

export interface DropdownProps extends PropsWithChildren {
    activeDropdown: string | null;
    dropdownList: ReactElement[];
    menuName: string;
    setActiveDropdown: (menuName: string | null) => void;
}

export interface ReportStreamHeaderProps extends PropsWithChildren {
    blueVariant?: boolean;
    isNavHidden?: boolean;
}

const ReportStreamHeader = ({ blueVariant }: ReportStreamHeaderProps) => {
    const { config } = useSessionContext();

    return (
        <>
            <GovBanner aria-label="Official government website" />
            <Header
                basic={true}
                className={classnames(styles.Navbar, {
                    [styles.NavbarBlueVariant]: blueVariant,
                    [styles.NavbarDefault]: !blueVariant,
                })}
            >
                <div className="usa-nav-container">
                    <div className="usa-navbar">
                        <Title>
                            <USLink href="/" title="Home" aria-label="Home">
                                ReportStream
                                {config.IS_PREVIEW && <span className={styles.ClientEnv}>{config.MODE}</span>}
                            </USLink>
                        </Title>
                    </div>
                </div>
            </Header>
        </>
    );
};

export default ReportStreamHeader;
