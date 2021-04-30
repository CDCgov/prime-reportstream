function requestFile( reportId ){
    let baseURL = window.location.origin.includes( "localhost") ? "http://localhost:7071" :
                  window.location.origin.includes( "staging" ) ? "https://staging.prime.cdc.gov" :
                  "https://prime.cdc.gov"

    let config = { headers: { 'Authorization': `Bearer ${window.jwt}` } }

    return axios.get(`${baseURL}/api/history/report/${reportId}`, config)
            .then( res => res.data )
            .then( csv => download( csv.content, csv.filename, csv.mimetype) )

  }