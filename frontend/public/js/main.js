$(document).ready(function() {
    $('.datatable').DataTable();

    $('.dt-sort1d').DataTable().order([1, 'desc']).draw();
});
