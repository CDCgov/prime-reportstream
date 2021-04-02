

async function fetchReports() 
{

    let config = {
        headers: { 
            'Authorization': `Bearer ${window.jwt}`
            }
        }
        
        const response = await axios.get('http://localhost:7071/api/history/report', config);
        return response.data;
};

