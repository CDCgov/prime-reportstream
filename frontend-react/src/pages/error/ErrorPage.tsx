import { useHistory } from 'react-router';
import { NotFound } from './NotFound'
import { UnsupportedBrowser } from './UnsupportedBrowser';

interface ErrorPageProps {
    code: string;
    content?: JSX.Element
}

/* INFO
   For consistency, when passing the code prop, please use these values
   e.g. <ErrorPage code={CODES.NOT_FOUND_404} />
 */
export enum CODES {
    UNSUPPORTED_BROWSER = "unsupported-browser",
    NOT_FOUND_404 = "not-found"
}

export function ErrorPage(props: ErrorPageProps) {
    const history = useHistory();
    /* INFO
       This is preferable to a switch statmenet due to readability
     */
    const codes = {
        "not-found": <NotFound />,
        "browser": <UnsupportedBrowser />
    }
    let content = codes[props.code]
    if (content === undefined) {
        history.push("/")
    }

    return (
        <div className="usa-section padding-top-6">
            <div className="grid-container">
                <div className="grid-row grid-gap">
                    {content}
                </div>
            </div>
        </div>
    )
}
