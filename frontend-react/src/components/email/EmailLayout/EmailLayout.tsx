import EmailContainer from "../EmailContainer/EmailContainer";
import { EmailFooter } from "../EmailFooter/EmailFooter";
import { EmailTableRow } from "../EmailTableRow/EmailTableRow";
import { oktaProperty } from "../utils";

export interface EmailLayoutProps extends React.PropsWithChildren {}

export function EmailLayout({ children }: EmailLayoutProps) {
    const env = "production" as const;
    const envName =
        env === "production" ? "ReportStream" : "ReportStream Staging";
    return (
        <div
            style={{
                backgroundColor: oktaProperty("brand.theme.secondaryColor"),
                margin: "0",
            }}
        >
            <EmailContainer type="outer">
                <EmailTableRow align="middle">
                    <td style={{ paddingTop: "30px", paddingBottom: "32px" }}>
                        <img
                            alt={envName}
                            src={oktaProperty("brand.theme.logo")}
                            height="37"
                        />
                    </td>
                </EmailTableRow>
                <EmailTableRow bgcolor="#ffffff">
                    <td>
                        <EmailContainer type="inner">{children}</EmailContainer>
                    </td>
                </EmailTableRow>
                <EmailFooter />
            </EmailContainer>
        </div>
    );
}
