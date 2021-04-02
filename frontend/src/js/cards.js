
async function fetchCards() {

    let config = {
        headers: { 
            'Authorization': `Bearer ${window.jwt}`
            }
        }

        const response = await Promise.all( [
            axios.get('http://localhost:7071/api/history/summary/tests', config).then( res => res.data )
        ] );

        console.log( response );
        return response;
};


