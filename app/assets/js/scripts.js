var headerHeight = 55;
var pageHeight = $(window).height() - headerHeight;
var lastScrollTop = 0;
var scrolling = false;

function fullHeight() {

	$('.full-height').css('height', pageHeight);
}

function log(log) {
	console.log(log)
}

$(window).resize(function() {
	pageHeight = $(window).height() - headerHeight;

	fullHeight();
});

$(document).ready(function() {
	fullHeight();
});

function submitOnReturn(form, textarea) {
	$(textarea).keydown(function(e) {
		if (e.keyCode === 13) {
			$(form).submit();
			return false;
		}
	})
}

$(document).keydown(function(e) {

	if (e.keyCode === 38) {
		// up arrow

		pageUp();
		return false;
	} else if (e.keyCode === 40) {
		// down arrow

		pageDown();
		return false;
	}
});
//
//$(window).scroll(function(e) {
//
//	var st = $(document).scrollTop();
//
//	if (st < lastScrollTop){
//		// scrolling up
//
//		scrollPageUp();
//		return false;
//	} else {
//		// scrolling down
//
//		scrollPageDown();
//		return false;
//	}
//
//});

$(window).bind('mousewheel DOMMouseScroll', function(e){

	var st = $(document).scrollTop();
	var wh = $(window).height();
	var dh = $(document).height();

	if (e.originalEvent.wheelDelta > 0 || e.originalEvent.detail < 0) {
		// scroll up

		if (st > 0) { // if it has reached the top let it scroll so that the bounce animation on osx/ios is preserved

			pageUp();
			return false;
		}
	}
	else {
		// scroll down

		if (st < dh - wh) { // if it has reached the bottom let it scroll so that the bounce animation on osx/ios is preserved
			pageDown();
			return false;
		}
	}
});

function pageDown() {

	var st = $(document).scrollTop();
	var currentPage = parseInt(st / pageHeight);

	if (!scrolling) {

		scrolling = true;

		$('html, body').animate({
			scrollTop: ((currentPage + 1) * pageHeight) + headerHeight
		}).promise().done(function() {

			scrolling = false;
			lastScrollTop = st;
		});
	} else {
		return false;
	}

}

function pageUp() {

	var st = $(document).scrollTop();
	var currentPage = parseInt(st / pageHeight);

	if (!scrolling) {

		scrolling = true;
		var header = 0;

		if (st >= (headerHeight * 2) + pageHeight) { header = headerHeight }

		$('html, body').animate({
			scrollTop: ((currentPage - 1) * pageHeight) + header
		}).promise().done(function() {

			scrolling = false;
			lastScrollTop = st;
		});
	} else {
		return false;
	}
}

function addFormActions(addUrl) {

	submitOnReturn('#home-inkle-form', '#inkle-textarea');

	$('#home-inkle-form').submit(function() {
		log('add');
		$.ajax({
			url: addUrl,
			data: $('#home-inkle-form').serialize(),
			type: "POST",
			success: function (e) {
				$('#inkles-wrapper').prepend(e);
				$('#inkle-textarea').val('');
				fullHeight();
				pageDown();
			},
			error: function(e) {
				alert('ERROR: ' + e);
			}
		});

		return false;
	});
}

function inkleActions(pageUuid, uuid, extendUrl, editUrl) {
	$('#page-'+ pageUuid +'-'+ uuid +'-extend-switch').click(function() {
		$('#page-'+ pageUuid +'-'+ uuid +'-extend-form-div').slideToggle();
		$('#page-'+ pageUuid +'-'+ uuid +'-extend-textarea').focus();
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-inkle-text').click(function() {

	}).mouseover(function () {
		$('#page-'+ pageUuid +'-'+ uuid +'-edit-switch').show();
	}).mouseleave(function() {
		$('#page-'+ pageUuid +'-'+ uuid +'-edit-switch').fadeOut();
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-edit-switch').click(function() {

		$('#page-'+ pageUuid +'-'+ uuid +'-inkle-text').hide();
		$('#page-'+ pageUuid +'-'+ uuid +'-edit-form-wrapper').show();
		$('#page-'+ pageUuid +'-'+ uuid +'-edit-textarea').focus();
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-extend-form-wrapper').focusout(function() {
		if ($('#page-'+ pageUuid +'-'+ uuid +'-extend-textarea').val == '') {
			$('#page-'+ pageUuid +'-'+ uuid +'-extend-switch').slideDown();
			$('#page-'+ pageUuid +'-'+ uuid +'-extend-form-div').slideUp();
		}
	});

	submitOnReturn('#page-'+ pageUuid +'-'+ uuid +'-extend-form', '#page-'+ pageUuid +'-'+ uuid +'-extend-textarea');
	submitOnReturn('#page-'+ pageUuid +'-'+ uuid +'-edit-form', '#page-'+ pageUuid +'-'+ uuid +'-edit-textarea');

	$('#page-'+ pageUuid +'-'+ uuid +'-edit-form').submit(function() {
		$.ajax({
			url: editUrl,
			type: "POST",
			data: $('#page-'+ pageUuid +'-'+ uuid +'-edit-form').serialize(),
			success: function (e) {
				$('#page-'+ pageUuid +'-'+ uuid +'-center-wrapper').replaceWith(e);
				fullHeight();
			},
			error: function () {
				alert("ERROR: "+ e)
			}
		});

		return false;
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-extend-form').submit(function() {
		log('extend');
		$.ajax({
			url: extendUrl,
			data: $('#page-'+ pageUuid +'-'+ uuid +'-extend-form').serialize(),
			type: "POST",
			success: function(e) {
				$('#page-'+ pageUuid +'-'+ uuid +'-children-wrapper').append(e);
				$('#page-'+ pageUuid +'-'+ uuid +'-extend-textarea').val('');
			},
			error: function(e) {
				alert("ERROR: " + e);
			}
		});

		return false;
	});


	$('#page-'+ pageUuid +'-'+ uuid +'-children-wrapper').bind('mousewheel DOMMouseScroll', function(e){
		if (e.originalEvent.wheelDelta > 0 || e.originalEvent.detail < 0) {
			// scroll up
			return false;
		}
		else {
			// scroll down
			return false;
		}
	});
}

function inkleClickActions(element, wrapper, renderUrl) {

	$(element).click(function() {
		$.ajax({
			url: renderUrl,
			type: 'POST',
			success: function (e) {
				$(wrapper).replaceWith(e);
				fullHeight();
			}
		});
 }).mouseover(function() {
 		$(element +'-counter').show();
 	}).mouseleave(function() {
 		$(element +'-counter').hide();

 	});
}


