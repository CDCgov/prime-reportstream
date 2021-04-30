async function fetchOrgName() {
    let config = { headers: { 'Authorization': `Bearer ${window.jwt}` } }
    let baseURL = window.location.origin.includes("localhost") ? "http://localhost:7071" :
        window.location.origin.includes("staging") ? "https://staging.prime.cdc.gov" :
            "https://prime.cdc.gov"
    const response = await Promise.all([
        axios.get(`${baseURL}/api/settings/organizations/${window.org.substring(2).replaceAll("_", "-")}`, config)
            .then(res => res.data)
            .then(org => org.description)
    ]);

    return response;
}

(async () => {
    const orgName = await fetchOrgName();
    document.getElementById("orgName").innerHTML += orgName;
})();