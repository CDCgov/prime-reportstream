import { format, parseISO } from "date-fns";

import { RSDelivery } from "../../../config/endpoints/deliveries";

interface Props {
    /* REQUIRED
    Passing in a report allows this component to extract key properties (type, sent,
    total, and expires) and display them on the DeliveryDetail page. */
    report: RSDelivery | undefined;
}

function DeliveryInfo(props: Props) {
    const { report } = props;

    if (!report) return <></>;
    return (
        <section className="margin-top-0 margin-bottom-5">
            <hr />
            <div id="details" className="grid-row grid-gap margin-top-0">
                <div className="tablet:grid-col">
                    <h4 className="text-base-darker text-normal margin-bottom-0">Report type</h4>
                    <p className="text-bold margin-top-0">{report.fileType}</p>
                    <h4 className="text-base-darker text-normal margin-bottom-0">Available to download</h4>
                    <p className="text-bold margin-top-0">
                        {}
                        {format(parseISO(report.batchReadyAt), "eeee, LLL dd, yyyy HH:mm")}
                    </p>
                </div>
                <div className="tablet:grid-col">
                    <h4 className="text-base-darker text-normal margin-bottom-0">Total tests reported</h4>
                    <p className="text-bold margin-top-0">{report.reportItemCount}</p>
                    <h4 className="text-base-darker text-normal margin-bottom-0">Download expires</h4>
                    <p className="text-bold margin-top-0">
                        {}
                        {format(parseISO(report.expires), "eeee, LLL dd, yyyy HH:mm")}
                    </p>
                </div>
            </div>
            <hr className="margin-top-3" />
        </section>
    );
}

export default DeliveryInfo;
