var simTable = $('.datatable-simulations').DataTable({
    "dom": '<"toolbar">frtip'
});

$('#DataTables_Table_0_filter').html('<button type="button" id="compare" class="btn btn-success disabled">Compare selected</button> ' +
    $('#DataTables_Table_0_filter').html());

var lastSelected = null;

$('.datatable-simulations tbody').on('click', 'tr', function() {
    if (simTable.rows('.active').data().length == 2) {
        if (lastSelected)
            lastSelected.toggleClass('active');
    }

    $(this).toggleClass('active');
    lastSelected = $(this);

    if (simTable.rows('.active').data().length == 2) {
        $('#compare').removeClass('disabled');
    } else {
        $('#compare').addClass('disabled');
    }
});

$('#compare').click(function() {
    var a = $(simTable.rows('.active').data()[0][0]).text();
    var b = $(simTable.rows('.active').data()[1][0]).text();
    window.location.replace("/simulations/compare/" + a + "/" + b);
    return false;
});
