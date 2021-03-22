var ctx = 'tests-administered';

var options = {
      legend: {
          display: false,
      },
      scales: {
          yAxes: [{
              ticks: { beginAtZero: false },
              display: false
          }],
          xAxes: [{
              display: false
          }]
      }
  }

var labels = ["2021-03-09", "2021-03-10","2021-03-11","2021-03-12","2021-03-13","2021-03-14","2021-03-15",]

var myLineChart = new Chart(ctx, {
  type: 'line',
  data: {
    labels: labels,
    datasets: [{ 
        data: [106,114,106,106,107,111,107],
        borderColor: "#4682B4",
        backgroundColor: "#B0C4DE",
        fill: 'origin',
        borderJoinStyle: "round"
      }
    ]
  },
  options: options
});
