import { useReportsFacilities } from "../../../hooks/network/History/DeliveryHooks";
import Table, { TableConfig } from "../../../components/Table/Table";

interface FacilitiesTableProps {
    /* REQUIRED
    Passing in a report allows this component to map through the facilities property
    to display a row per facility on the FaclitiesTable. */
    reportId: string;
}

function DeliveryFacilitiesTable(props: FacilitiesTableProps) {
    const { reportId }: FacilitiesTableProps = props;
    const { reportFacilities } = useReportsFacilities(reportId);

    const tableConfig: TableConfig = {
        columns: [
            { dataAttr: "facility", columnHeader: "Facility" },
            { dataAttr: "location", columnHeader: "Location" },
            { dataAttr: "CLIA", columnHeader: "CLIA" },
            { dataAttr: "total", columnHeader: "Total tests" },
            { dataAttr: "positive", columnHeader: "Total positive" },
        ],
        rows: reportFacilities!!,
    };

    return (
        <section id="facilities" className="margin-bottom-5">
            <h2>Facilities reporting ({reportFacilities?.length || 0})</h2>
            <Table config={tableConfig} />
        </section>
    );
}

export default DeliveryFacilitiesTable;
