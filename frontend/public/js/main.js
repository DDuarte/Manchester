$(document).ready(function() {
    $('.datatable').DataTable();
});

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

});

/*
$(function () {
    $('#visitsPerCategoryContainer').highcharts({
        data: {
            table: 'visitsPerCategory'
        },
        chart: {
            type: 'column'
        },
        title: {
            text: ''
        },
        xAxis: {
            type: 'category',
            labels: {
                rotation: -45
            }
        },
        yAxis: {
            allowDecimals: false,
            title: {
                text: 'Count'
            }
        },
        legend: {
            enabled: false
        },
        tooltip: {
            formatter: function () {
                return '<b>' + this.point.name + '</b>: ' + this.point.y;
            }
        }
    });
});*/
