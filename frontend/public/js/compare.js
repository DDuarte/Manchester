$(document).ready(function() {
    $('#dt-compare-general').DataTable({
        "paging": false,
        "ordering": false,
        "info": false,
        "columnDefs": [
            {className: "dt-right", "targets": [1]}
        ]
    });

    var options = {
        chart: {
            renderTo: 'visitsPerCategoryContainer',
            type: 'column'
        },
        series: [{
            name: 'Categories A',
            colorByPoint: true,
            data: []
        }, {
            name: 'Categories B',
            colorByPoint: true,
            data: []
        }],
        drilldown: {
            series: [{}, {}]
        },
        legend: {
            enabled: true
        },
        yAxis: {
            title: {
                text: 'Visits'
            }

        },
        xAxis: {
            type: 'category'
        },
        title: {
            text: ''
        },
        subtitle: {
            text: 'Click the columns to view sub-categories.'
        },
        plotOptions: {
            series: {
                borderWidth: 0,
                dataLabels: {
                    enabled: true,
                    format: '{point.y}'
                }
            }
        }
    };

    var match = /.+compare\/(\w+)\/(\w+)/.exec(window.location.pathname);
    var id1 = match[1];
    var id2 = match[2];

    $.getJSON('/simulations/' + id1 + '/visitsPerCategory', function(data1) {

        $.getJSON('/simulations/' + id2 + '/visitsPerCategory', function(data2) {
            options.series[0].data = data1.series;
            options.series[1].data = data2.series;
            // options.drilldown.series[0] = data1.drilldown;
            // options.drilldown.series[1] = data2.drilldown;
            var chart = new Highcharts.Chart(options);
        });
    });
});
