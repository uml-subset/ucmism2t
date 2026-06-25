$(document).ready(function() {
    $('table.syntaxmapping').DataTable( {
		// column( 0 ).visible( true, false );
		autoWidth:		false,
		scrollX:		true,
		scrollY:		"77vh",
        scrollCollapse:	false,
        paging:         false,
		ordering:		false,
        fixedColumns:	{ left: 1 },
		// https://datatables.net/reference/option/dom
        // dom: 'Bfrtip',
        dom:			'Bt', // B - initialise button, f - filtering input, r -  processing display element, t - table, i - Table information summary, p - pagination control
		columnDefs: [
			// { "targets": 0, "width": "20%" }, impact, but still recomputation after click
			// { "width": "200px", "targets": 0 }, no impact!
			// { "width": "880px", "targets": 1 },
			// { "width": "880px", "targets": 2 },
            { targets: 0, className: 'noVis' }
        ],
        buttons: [
			{
				extend: 'columnsToggle',
                columns: ':not(.noVis)'
            },
			{
                extend: 'colvisGroup',
                text: 'Show all',
                show: ':hidden'
            }
        ]
	} );
} );