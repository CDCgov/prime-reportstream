
function fetchCards(){
        return [{
            id: "tests-administered", 
            title: "Testing" ,
            subtitle: "Tests received",
            daily: 2497,
            last: 9348,
            positive: false,
            change: -897,
            pct_change: 9.6,
            data: [ 1, 2, 3, 4, 5, 4, 3 ]
        }];
}

let cards = fetchCards();

/*

module.exports = async function() {

    let config = {
            headers: {'Access-Control-Allow-Origin': '*'}
        };
        

        const response = await Promise.all( [
            axios.get('http://localhost:7071/api/history/summary/positive', config).then( res => res.data ),
            axios.get('http://localhost:7071/api/history/summary/tests', config).then( res => res.data ),
            axios.get('http://localhost:7071/api/history/summary/facilities', config).then( res => res.data ),
        ] );

        console.log( response );
        return response;
};
*/

