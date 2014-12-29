
function fullHeight() {
	$('.full-height').css('height', $(window).height());
}

$(window).resize(function() {
	fullHeight();
});

$(document).ready(function() {
	fullHeight();
});

$(document).onkeyup(function(e) {
	console.log(e.keyCode)
});