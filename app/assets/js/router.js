$(document).ready(function() {
	$('.add-modal-switch').click(function() {
		$("#add-modal").modal('show');
	});

	$('#main-menu-btn').click(function() {
		$("#menu-modal").modal('show');
	});

	$('.go-to-origins').click(function() {
		renderRoute(jsRoutes.controllers.Apps.templateOrigins(), '/origins', "Inklin • Origins");
		$('#menu-modal').modal('hide');

		return false;
	});

	$('.home-link').click(function() {
		renderRoute(jsRoutes.controllers.Apps.templateHome(), '/', "Inklin");
		$('#inkle-textarea').focus();

		return false;
	});

	$('.go-to-groups').click(function() {
		renderRoute(jsRoutes.controllers.Groups.templateList(), '/groups', "Inklin • Groups");

		$('#main-dropdown-menu' ).dropdown('hide');
		$('#menu-modal').modal('hide');

		return false;
	});

	$("#menu-modal").click(function(e) {
		var target = e.target;
		var searchForm = $("#menu-search-form");

		if (
			!$(target).is(searchForm) && !$(target).parents().is(searchForm)
		) {
			$('#menu-modal').modal('hide');
		}
	});

	$("#add-modal").click(function(e) {
		var target = e.target;
		var inkleForm = $("#modal-inkle-form");

		if (
			!$(target).is(inkleForm) && !$(target).parents().is(inkleForm)
		) {
			$('#add-modal').modal('hide');
		}
	});
});

function renderRoute(templateAddress, route, title) {
	loader(true);
	$.ajax(templateAddress)
	  .done(function(e) {
			loader(false);

			window.history.pushState('obj', title, route);
			document.title = title;
			$("#template-container").html(e)
		}).fail(function() {
			loader(false);

			alert("oops");
		});
}