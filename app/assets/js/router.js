$(document).ready(function() {
	$('#main-add-btn').click(function() {
		$("#add-modal").modal('show');

		return false;
	});

	$('#main-origin-btn').click(function() {
		renderRoute(jsRoutes.controllers.Apps.templateOrigins(), '/origins', "Inklin • Origins");

		return false;
	});
	$('.home-link').click(function() {
		renderRoute(jsRoutes.controllers.Apps.templateHome(), '/', "Inklin");

		return false;
	});
});

function renderRoute(templateAddress, route, title) {
	mainLoader(true);
	$.ajax(templateAddress)
	  .done(function(e) {
			mainLoader(false);

			window.history.pushState('obj', '', route);
			document.title = title;
			$("#template-container").html(e)
		}).fail(function() {
			mainLoader(false);

			alert("oops");
		});
}