import { useHistory } from 'react-router';

interface ErrorPageProps {
    content?: JSX.Element
}

export function ErrorPage(props: ErrorPageProps) {
    const history = useHistory();
    if (props.content === undefined) {
        history.push("/")
    }

    return (
        <div className="usa-section padding-top-6">
            <div className="grid-container">
                <div className="grid-row grid-gap">
                    {props.content}
                </div>
            </div>
        </div>
    )
}
