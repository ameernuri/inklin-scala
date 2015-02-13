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

		jsRoutes.controllers.Inkles.delete().ajax({
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

	groupActions();
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

function groupActions() {

	$('#modal-group-form').submit(function() {

		loader();

		jsRoutes.controllers.Groups.create("json").ajax({
			data: $("#modal-group-form").serialize(),
			success: function(e) {

				renderRoute(jsRoutes.controllers.Groups.templateView(e.uuid), "/origins/"+ e.uuid, "Inklin • "+ e.name);

				loader(false);
				$("#create-group-modal").modal('hide');
				$("#create-group-text-name").val('');
			},
			error: function(e) {
				loader(false);

				alert("oops.");
			}
		});

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

function groupInkleAddActions() {

	submitOnReturn('#group-inkle-form', '#inkle-textarea');

	$('#group-inkle-form').submit(function() {
		log('add');

		loader();

		jsRoutes.controllers.Inkles.createInGroup("json").ajax({
			data: $("#group-inkle-form").serialize(),
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

	var inkle = $('#page-'+ pageUuid +'-'+ inkleUuid +'-inkle');

	$(button).click(function () {
		loader();
		jsRoutes.controllers.Inkles.getPageOfChildren(inkleUuid, pageUuid, pageNumber).ajax({
			success: function (e) {
				loader(false);
				inkle.find('.children-wrapper').html(e)
			},
			error: function () {
				alert("error");
				loader(false);
			}
		});
	})
}

function inkleActions(pageUuid, uuid) {

	var inkle = $('#page-'+ pageUuid +'-'+ uuid +'-inkle');

	inkle.find('.extend-form-switch').click(function() {
		inkle.find('.extend-form-div').slideToggle();
		inkle.find('.inkle-textarea').focus();
		inkle.find('.extend-form-switch').hide();
	});

	inkle.find('.delete-inkle-button').click(function() {
		$('#inkle-delete').modal('show');
		$("#delete-inkle-uuid").val(uuid);
		$("#deleted-inkle-page").val(inkle.attr('id'));
		return false;
	});

	inkle.find('.change-parent-button').click(function() {
		$('#change-parent').modal('show');
		$("#change-parent-inkle-uuid").val('#page-'+ pageUuid +'-'+ uuid +'-inkle');
		return false;
	});

	inkle.find('.inkle-text').click(function() {
		$(this).hide();
		inkle.find('.edit-form-wrapper').fadeIn();
		inkle.find('.edit-textarea').focus().trigger('autosize.resize');

	});

	$(document).click(function(e) {
    var target = e.target;

    if (
	    !$(target).is(inkle.find('.edit-form-wrapper')) &&
	    !$(target).parents().is(inkle.find('.edit-form-wrapper')) &&
	    !$(target).is(inkle.find('.inkle-text')) &&
	    !$(target).parents().is(inkle.find('.inkle-text'))
    ) {
	    inkle.find('.inkle-text').fadeIn();
	  	inkle.find('.edit-form-wrapper').hide();
    }

		if (
	   !$(target).is(inkle.find('.extend-form-wrapper')) &&
	   !$(target).parents().is(inkle.find('.extend-form-wrapper'))
	  ) {
			inkle.find('.extend-form-switch').show();
			inkle.find('.extend-form-div').slideUp();
	  }
	});

	inkle.find('.edit-textarea').keydown(function (e) {
		if (e.keyCode === 27) {
			inkle.find('.inkle-text').fadeIn();
			inkle.find('.edit-form-wrapper').hide();
			return false;
		}
	});

	inkle.find('.inkle-textarea').focusout(function() {
		if (inkle.find('.inkle-textarea').val == '') {
			inkle.find('.extend-form-switch').slideDown();
			inkle.find('.extend-form-div').slideUp();
		}
	}).keydown(function (e) {
		if (e.keyCode === 27) {
			inkle.find('.extend-form-switch').show();
			inkle.find('.extend-form-div').slideUp();
		}
	});

	submitOnReturn(inkle.find('.extend-form'), inkle.find('.inkle-textarea'));
	submitOnReturn(inkle.find('.edit-form'), inkle.find('.edit-textarea'));

	inkle.find('.edit-form').submit(function() {
		loader();
		jsRoutes.controllers.Inkles.edit(uuid, pageUuid).ajax({
			data: inkle.find('.edit-form').serialize(),
			success: function (e) {
				inkle.find('.inkle-center-wrapper').replaceWith(e);
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

	inkle.find('.extend-form').submit(function() {
		log('extend');

		loader();
		jsRoutes.controllers.Inkles.extend(uuid, pageUuid).ajax({
			data: inkle.find('.extend-form').serialize(),
			success: function(e) {
				$('.tour').hide();
				inkle.find('.children-wrapper').append(e);
				inkle.find('.inkle-textarea').val('');

				loader(false);
			},
			error: function(e) {
				alert("ERROR: " + e);

				loader(false);
			}
		});

		return false;
	});
}

function inkleClickActions(element, pageUuid, inkleUuid, wrapper) {

	$(element).click(function() {

		loader();
		jsRoutes.controllers.Inkles.getInkle(inkleUuid).ajax({
			success: function (e) {
				var element = $(e);
				$(wrapper).replaceWith(element);
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