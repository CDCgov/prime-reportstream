(async () => {
    const cards = await fetchCards();
    cards.forEach(card => {
        document.getElementById("cards").innerHTML +=
            `<div class="tablet:grid-col-6">
            <div class="usa-card__container">
              <div class="usa-card__body">
                <h4 class="text-gray-30 margin-bottom-0">${card.title}</h4>
                <h3 class="text-bold margin-top-0">${card.subtitle}</h3>
                <h4 class="text-gray-30 margin-bottom-0">Last 24 hours</h4>
                <p class="text-bold margin-top-0">${card.daily} &nbsp; &nbsp; &nbsp; <span class="text-heavy ${card.positive ? "text-green" : "text-red"}">
                    ${card.positive ? "&#8599;" : "&#8600;"} ${card.change.toFixed(2)}
                  </span></p>
                <h4 class="text-gray-30 margin-bottom-0">Last 7 days (average)</h4>
                <p class="text-bold margin-top-0">${card.last}</p>
                <canvas id="${card.id}" width="200" height="40"></canvas>
              </div>  
            </div>
          </div>`
    });
    var ctx = 'summary-tests';
    var options = {
        plugins: {
            legend: {
                display: false,
            }
        },
        scales: {
            y: {
                ticks: {
                    beginAtZero: false
                },
                display: false,
            },
            x: {
                display: false,
            }
        }
    }

    var labels = [];
    for (var i = 7; i >= 0; i--) {
        labels.push(moment().subtract(i, 'days').format("YYYY-MM-DD"))
    }

    var myLineChart = new Chart(ctx, {
        type: 'line',
        data: {

            labels: labels,
            datasets: [{
                data: cards[0].data,
                borderColor: "#4682B4",
                backgroundColor: "#B0C4DE",
                fill: 'origin',
                borderJoinStyle: "round"
            }]
        },
        options: options
    });

})();
