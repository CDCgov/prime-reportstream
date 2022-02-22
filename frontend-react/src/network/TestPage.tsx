import React from "react";
import { HistoryApi } from "./api/History";
import NetworkCache from "./cache/NetworkCache";
import { useNetwork } from "./hooks/useNetwork";

function TestPage() {
    const { loading, data, status, message } = useNetwork(HistoryApi.list());

    return <div>TestPage</div>;
}

export default TestPage;
