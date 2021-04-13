async function fetchReports() {
    let config = { headers: {  'Authorization': `Bearer ${window.jwt}`} }        
    return await axios.get('http://localhost:7071/api/history/report', config);
};

