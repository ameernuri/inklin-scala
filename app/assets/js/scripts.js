var headerHeight = 55;
var pageHeight = $(window).height() - headerHeight;
var lastScrollTop = 0;
var scrolling = false;

$(document).bind('DOMSubtreeModified', function() {
	modifyActions();
});

$(document).ready(function() {
	mainActions();
});

function mainActions() {
	modifyActions();

	setTimeout(function() {
		$(".flash").fadeOut(2000)
	}, 5000);

	$("#flash-container").click(function() {
		$("#flash-content").html("");
		$(this).fadeOut();
	});

	submitOnReturn('#modal-inkle-form', '#modal-inkle-textarea');

	$('#modal-inkle-form').submit(function() {
		log('add');

		loader();

		jsRoutes.controllers.Inkles.create("json").ajax({
			data: $("#modal-inkle-form").serialize(),
			success: function(e) {
				console.log(e);

				renderRoute(jsRoutes.controllers.Inkles.templateOrigin(e.uuid), "/origins/"+ e.uuid, "Inklin");

				loader(false);
				$("#add-modal").modal('hide');
				$("#modal-inkle-textarea").val('');
			},
			error: function(e) {
				loader(false);

				alert("oops.");
			}
		});

		return false;
	});

	$("#popover-pad").click(function() {
		hidePopover();
	});

	$(document).keydown(function(e) {
		if (e.keyCode === 27) {
			hidePopover();
		}
	});

	$('.popover-close-button').click(function() {
		hidePopover();
		return false;
	});

	$('#delete-inkle-form').submit(function() {
		loader();

		$.ajax({
			url: "/inkles/delete",
			type: "POST",
			data: $('#delete-inkle-form').serialize(),
			success: function (e) {
				loader(false);
				$("#inkle-delete").modal('hide');
				if (e == "deleted") {

					$($('#deleted-inkle-page').val()).slideUp();
				} else {
					$($('#deleted-inkle-page').val()).html(e);
				}

				fullHeight();
			},
			error: function (e) {
				alert("ERROR: "+ e);

				loader(false);
				$("#inkle-delete").modal('hide');
			}
		});

		return false;
	});

	$('#add-modal').on('shown.bs.modal', function (e) {
	  $('#modal-inkle-textarea').focus();
	});

	$('#inkle-delete').on('shown.bs.modal', function (e) {
	  $('#inkle-delete-radio-keep').prop('checked', true);
	});

	$('#create-group-modal').on('shown.bs.modal', function (e) {
	  $('#create-group-text-name').focus();
	});
}

function modifyActions() {
	$(function () {
	  $('[data-toggle="tooltip"]').tooltip();
	});

	var textarea = $('textarea');

	textarea
		.autosize({append: false})
		.show()
		.trigger('autosize.resize');

	fullHeight();

	$('#create-group-button').click(function() {
		$("#create-group-modal").modal('show');

		return false;
	});
}

function homeInkleAddActions() {

	submitOnReturn('#home-inkle-form', '#inkle-textarea');

	$('#home-inkle-form').submit(function() {
		log('add');

		loader();

		jsRoutes.controllers.Inkles.create("json").ajax({
			data: $("#home-inkle-form").serialize(),
			success: function(e) {
				console.log(e);

				renderRoute(jsRoutes.controllers.Inkles.templateOrigin(e.uuid), "/origins/"+ e.uuid, "Inklin");

				loader(false);
			},
			error: function(e) {
				loader(false);

				alert("oops.");
			}
		});

		return false;
	});
}

function loader(show) {
	show = typeof show !== 'undefined' ? show : true;

	var loader = $("#main-loader");

	if (!show) {
		loader.hide()
	} else {
		loader.show();
	}
}

function popover(element) {
	$(element).show().css("top", $(window).scrollTop() + 100);
	$("#popover-pad").show();
}

function hidePopover() {
	$(".popover-container").fadeOut();
	$("#popover-pad").fadeOut();
}
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

function childrenPaginationActions(button, pageUuid, inkleUuid, pageNumber) {

	$(button).click(function () {
		loader();
		jsRoutes.controllers.Inkles.getPageOfChildren(inkleUuid, pageUuid, pageNumber).ajax({
			success: function (e) {
				loader(false);
				$('#page-'+ pageUuid +'-'+ inkleUuid +'-children-wrapper').html(e)
			},
			error: function () {
				alert("error");
				loader(false);
			}
		});
	})
}

function inkleActions(pageUuid, uuid) {

	$('#page-'+ pageUuid +'-'+ uuid +'-extend-switch').click(function() {
		$('#page-'+ pageUuid +'-'+ uuid +'-extend-form-div').slideToggle();
		$('#page-'+ pageUuid +'-'+ uuid +'-extend-textarea').focus();
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-delete-button').click(function() {
		$('#inkle-delete').modal('show');
		$("#delete-inkle-uuid").val(uuid);
		$("#deleted-inkle-page").val('#page-'+ pageUuid +'-'+ uuid +'-inkle');
		return false;
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-change-parent-button').click(function() {
		$('#change-parent').modal('show');
		$("#change-parent-inkle-uuid").val(uuid);
		return false;
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-center-wrapper').click(function() {

	});

	$('#page-'+ pageUuid +'-'+ uuid +'-edit-switch').click(function() {
		$('#page-'+ pageUuid +'-'+ uuid +'-inkle-text').hide();
		$('#page-'+ pageUuid +'-'+ uuid +'-edit-form-wrapper').fadeIn();
		$('#page-'+ pageUuid +'-'+ uuid +'-edit-textarea').focus().trigger('autosize.resize');
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-inkle-text').click(function() {
		$(this).hide();
		$('#page-'+ pageUuid +'-'+ uuid +'-edit-form-wrapper').fadeIn();
		$('#page-'+ pageUuid +'-'+ uuid +'-edit-textarea').focus().trigger('autosize.resize');
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-edit-form-cancel').click(function() {

		$('#page-'+ pageUuid +'-'+ uuid +'-inkle-text').fadeIn();
		$('#page-'+ pageUuid +'-'+ uuid +'-edit-form-wrapper').hide();
		return false;
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-edit-textarea').keydown(function (e) {
		if (e.keyCode === 27) {

			$('#page-'+ pageUuid +'-'+ uuid +'-inkle-text').fadeIn();
			$('#page-'+ pageUuid +'-'+ uuid +'-edit-form-wrapper').hide();
			return false;
		}
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-extend-textarea').focusout(function() {
		if ($('#page-'+ pageUuid +'-'+ uuid +'-extend-textarea').val == '') {
			$('#page-'+ pageUuid +'-'+ uuid +'-extend-switch').slideDown();
			$('#page-'+ pageUuid +'-'+ uuid +'-extend-form-div').slideUp();
		}
	});

	submitOnReturn('#page-'+ pageUuid +'-'+ uuid +'-extend-form', '#page-'+ pageUuid +'-'+ uuid +'-extend-textarea');
	submitOnReturn('#page-'+ pageUuid +'-'+ uuid +'-edit-form', '#page-'+ pageUuid +'-'+ uuid +'-edit-textarea');

	$('#page-'+ pageUuid +'-'+ uuid +'-edit-form').submit(function() {
		loader();
		jsRoutes.controllers.Inkles.edit(uuid, pageUuid).ajax({
			data: $('#page-'+ pageUuid +'-'+ uuid +'-edit-form').serialize(),
			success: function (e) {
				$('#page-'+ pageUuid +'-'+ uuid +'-center-wrapper').replaceWith(e);
				fullHeight();

				loader(false);
			},
			error: function (e) {
				alert("ERROR: "+ e);

				loader(false);
			}
		});

		return false;
	});

	$('#page-'+ pageUuid +'-'+ uuid +'-extend-form').submit(function() {
		log('extend');

		loader();
		jsRoutes.controllers.Inkles.extend(uuid, pageUuid).ajax({
			data: $('#page-'+ pageUuid +'-'+ uuid +'-extend-form').serialize(),
			success: function(e) {
				$('.tour').hide();
				$('#page-'+ pageUuid +'-'+ uuid +'-children-wrapper').append(e);
				$('#page-'+ pageUuid +'-'+ uuid +'-extend-textarea').val('');

				loader(false);
			},
			error: function(e) {
				alert("ERROR: " + e);

				loader(false);
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

function inkleClickActions(element, pageUuid, inkleUuid, wrapper) {

	$(element).click(function() {

		loader();
		jsRoutes.controllers.Inkles.getInkle(inkleUuid).ajax({
			success: function (e) {
				$(wrapper).css("background", "red").replaceWith(e);
				fullHeight();

				loader(false);
			},
			error: function() {

				loader(false);
			}
		});
 }).mouseover(function() {
 		$(element +'-counter').show();
	}).mouseleave(function() {
	  $(element +'-counter').hide();

	});
}


