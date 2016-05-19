$(document).ready(function() {

    var options = {
        chart: {
            renderTo: 'visitsPerCategoryContainer',
            type: 'column'
        },
        series: [{
            name: 'Categories',
            colorByPoint: true,
            data: []
        }],
        drilldown: {
            series: [{}]
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

    $.getJSON(window.location.pathname + '/visitsPerCategory', function(data) {
        options.series[0].data = data.series;
        options.drilldown.series = data.drilldown;
        var chart = new Highcharts.Chart(options);
    });

    $('.dt-sort1d').DataTable().order([1, 'desc']).draw();

    $('#dt-compare-general').DataTable({
        "paging":   false,
        "ordering": false,
        "info":     false,
        "columnDefs": [
            { className: "dt-right", "targets": [ 1 ] }
        ]
    });
});
