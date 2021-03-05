module.exports = async function() {
    return [ 
        { 
            id: "positive-cases",
            title: "Cases" ,
            subtitle: "People tested positive",
            daily: 329,
            last: 1294,
            positive: true,
            change: -267,
            pct_change: 20.6,
            data: [ 1, 2, 3, 4, 5, 4, 3 ]
        },  {
            id: "tests-administered", 
            title: "Testing" ,
            subtitle: "Tests administered",
            daily: 2497,
            last: 9348,
            positive: false,
            change: -897,
            pct_change: 9.6,
            data: [ 1, 2, 3, 4, 5, 4, 3 ]
        }, {
            id: "facilities",
            title: "Facilities" ,
            subtitle: "New testing locations",
            daily: 4,
            last: 12,
            positive: true,
            change: 4,
            pct_change: 15.9,
            data: [ 1, 2, 3, 4, 5, 4, 3 ]    
        }];
}