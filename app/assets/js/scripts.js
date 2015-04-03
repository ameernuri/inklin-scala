var headerHeight = 55;
var pageHeight = $(window).height() - headerHeight;
var lastScrollTop = 0;
var scrolling = false;

RegExp.escape = function(str) {
  var specials = /[.*+?|()\[\]{}\\$^]/g; // .*+?|()[]{}\$^
  return str.replace(specials, "\\$&");
};

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

	submitOnReturn('#modal-inkle-form');

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

				alert(e.error);
			}
		});

		return false;
	});

	function fetchSearchSuggestions() {

		var value = $('#modal-search-input').val();

		jsRoutes.controllers.Inkles.search(value).ajax({
			success: function (e) {
				$('#modal-search-suggestions-wrap').html('');
				$.each(e, function () {
					$.each(this, function (a, b) {
						if (b.inkle != '') {

							var words = value.split(' ');

							words.sort(function(a, b){
							  return b.length - a.length;
							});

							var inkle = b.inkle;

							$.each(words, function() {

								var fixed = this.trim();
								var regex = new RegExp('(' + RegExp.escape(fixed) + ')(?!>|b>)', 'gi');

								if (fixed != '') {

									inkle = (inkle).replace(regex, "<b>$1</b>");
								}
							});

							var result = '';

							if (b.uuid == b.originUuid) {
								result =
								'<span class="glyphicon glyphicon-record"></span> ' + inkle;
							} else {
								result =
								'<span class="result-origin">' +
								'<span class="glyphicon glyphicon-record"></span> ' + b.origin +
								'</span>' +
								' <span class="glyphicon glyphicon-chevron-right"></span> ' + inkle;
							}

							$('#modal-search-suggestions-wrap').append(
								'<div class="search-result" onclick="searchResultClickActions(\''+ b.originUuid +'\',\''+ b.uuid +'\')">' + result + '</div>'
							);
						}
					});
				})
			},
			error: function (e) {
				alert("ERROR: "+ e);
			}
		});
	}

	function fetchLinkSuggestions() {

		var value = $('#link-inkle-input').val();
		var uuid = $('#link-inkle-uuid').val();
		var pageUuid = $('#link-inkle-page-uuid').val();

		jsRoutes.controllers.Inkles.searchLinkable(uuid, value).ajax({
			success: function (e) {
				if (value = $('#link-inkle-input').val()) {
					$('#link-inkle-suggestions-wrap').html('');
					$.each(e, function () {
						$.each(this, function (a, b) {
							if (b.inkle != '') {

								var words = value.split(' ');

								words.sort(function(a, b){
								  return b.length - a.length;
								});

								var inkle = b.inkle;

								$.each(words, function() {

									var fixed = this.trim();
									var regex = new RegExp('(' + RegExp.escape(fixed) + ')(?!>|b>)', 'gi');

									if (fixed != '') {

										inkle = (inkle).replace(regex, "<b>$1</b>");
									}
								});

								var result = '';

								if (b.uuid == b.originUuid) {
									result =
									'<span class="glyphicon glyphicon-record"></span> ' + inkle;
								} else {
									result =
									'<span class="result-origin">' +
									'<span class="glyphicon glyphicon-record"></span> ' + b.origin +
									'</span>' +
									' <span class="glyphicon glyphicon-chevron-right"></span> ' + inkle;
								}

								$('#link-inkle-suggestions-wrap').append(
									'<div class="search-result create-link" link-to="'+ uuid +'" onclick="log(\'what!\');linkInkle(\'' + uuid + '\',\'' + b.uuid + '\',\'' + pageUuid + '\',\'' + '#page-'+ pageUuid +'-'+ uuid +'-inkle' + '\');">'+ result +'</div>'
								);
							}
						});
					});
				}
			},
			error: function (e) {
				alert("ERROR: "+ e);
			}
		});
	}

	function fetchParentSuggestions() {

		var value = $('#change-parent-input').val();
		var uuid = $('#change-parent-uuid').val();
		var pageUuid = $('#change-parent-page-uuid').val();

		jsRoutes.controllers.Inkles.searchLinkable(uuid, value).ajax({
			success: function (e) {
				if (value = $('#change-parent-input').val()) {
					$('#change-parent-suggestions-wrap').html('');
					$.each(e, function () {
						$.each(this, function (a, b) {
							if (b.inkle != '') {

								var words = value.split(' ');

								words.sort(function(a, b){
								  return b.length - a.length;
								});

								var inkle = b.inkle;

								$.each(words, function() {

									var fixed = this.trim();
									var regex = new RegExp('(' + RegExp.escape(fixed) + ')(?!>|b>)', 'gi');

									if (fixed != '') {

										inkle = (inkle).replace(regex, "<b>$1</b>");
									}
								});

								var result = '';

								if (b.uuid == b.originUuid) {
									result =
									'<span class="glyphicon glyphicon-record"></span> ' + inkle;
								} else {
									result =
									'<span class="result-origin">' +
									'<span class="glyphicon glyphicon-record"></span> ' + b.origin +
									'</span>' +
									' <span class="glyphicon glyphicon-chevron-right"></span> ' + inkle;
								}

								$('#change-parent-suggestions-wrap').append(
									'<div class="search-result create-link" link-to="'+ uuid +'" onclick="changeParent(\'' + uuid + '\',\'' + b.uuid + '\',\'' + pageUuid + '\',\'' + '#page-'+ pageUuid +'-'+ uuid +'-inkle' + '\');">'+ result +'</div>'
								);
							}
						});
					});
				}
			},
			error: function (e) {
				alert("ERROR: "+ e);
			}
		});
	}

	$('#modal-menu-inkle-search').keyup(function () {
		var value = $('#modal-menu-inkle-search').val();

		if (value != '') {

			$(this).val('');

			$('#modal-search-input').val(value);

			$('#menu-modal').modal('hide');
			$('#search-modal').modal('show');
		}

	});

	$('#search-modal').on('shown.bs.modal', function (e) {

	  $('#modal-search-input').focus();
		fetchSearchSuggestions();
	});

	$('#modal-search-input').bind('input', function () {
		fetchSearchSuggestions();
	});

	$('#link-inkle-input').bind('input', function () {
		fetchLinkSuggestions();
	});

	$('#change-parent-input').bind('input', function () {
		fetchParentSuggestions();
	});

	$('.create-link').click(function () {
		alert($(this).getAttribute('link-to'))
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

	$('#menu-modal').on('shown.bs.modal', function (e) {
	  $('#modal-menu-inkle-search').focus();
	});

	$('#inkle-delete').on('shown.bs.modal', function (e) {
	  $('#inkle-delete-radio-keep').prop('checked', true);
	});

	$('#create-group-modal').on('shown.bs.modal', function (e) {
	  $('#create-group-text-name').focus();
	});

	submitOnReturn("#modal-group-form")

	groupActions();
}

function modifyActions() {
	$(function () {
	  $('[data-toggle="tooltip"]').tooltip();
		$('[data-toggle="popover"]').popover();
	});

	$('.user-profile-popover').popover({
		trigger: 'hover'
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

				alert(e.error);
			}
		});

		return false;
	});
}

function editGroupActions(uuid) {

	var editForm = $('#group-'+ uuid +'-form');
	var info = $('#group-'+ uuid +'-info');

	submitOnReturn(editForm);

	info.click(function() {
		$(this).hide();
		editForm.fadeIn();
		editForm.find('input[type=text]').focus();
		editForm.find('textarea').trigger('autosize.resize');

		editForm.submit(function () {
			loader();
			jsRoutes.controllers.Groups.update(uuid).ajax({
				data: editForm.serialize(),
				success: function (e) {
					info.fadeIn();
					editForm.hide();

					info.find('.group-name').html(e.name);
					info.find('.group-description').html(e.description);

					loader(false);
				},
				error: function (e) {
					alert("ERROR: "+ e);

					loader(false);
				}
			});

			return false;
		});
	});

	editForm.find('textarea, input').keydown(function (e) {
		if (e.keyCode === 27) {
			editForm.hide();
			info.fadeIn();
			return false;
		}
	});

	$(document).click(function(e) {
		var target = e.target;

		if (
			!$(target).is(editForm) && !$(target).parents().is(editForm) &&
			!$(target).is(info) && !$(target).parents().is(info)
		) {
			editForm.hide();
			info.fadeIn();
		}
	});
}

function searchResultClickActions(origin, inkle) {
	$('#search-modal').modal('hide');
	renderRoute(jsRoutes.controllers.Inkles.templateView(inkle), "/origins/"+ origin +"/inkles/"+ inkle, "Inklin");
}

function homeInkleAddActions() {

	submitOnReturn('#home-inkle-form');

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

				alert(e.error);
			}
		});

		return false;
	});
}

function groupInkleAddActions() {

	submitOnReturn('#group-inkle-form');

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

				alert(JSON.parse(e).b);
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

function linkInkle(from, to, pageUuid, inkleId) {

	var inkle = $(inkleId);
	jsRoutes.controllers.Inkles.link(from, to, pageUuid).ajax({
		success: function(e) {
			$('#link-inkle').modal('hide');
			inkle.find('.inkle-textarea').val('');
			$('#link-inkle-input').val('');
			inkle.find('.children-wrapper').append(e);

			loader(false);
		},
		error: function () {
			alert("something's up!")
		}
	});
}

$(window).resize(function() {
	pageHeight = $(window).height() - headerHeight;

	fullHeight();
});

$(document).ready(function() {
	fullHeight();
});

function submitOnReturn(form) {
	var formSelector = $(form);

	$(formSelector).find('textarea').keydown(function(e) {
		if (e.keyCode === 13) {
			formSelector.submit();
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

	var target = e.target;
	var fullHeight = $('.full-height');

	if ($(target).is(fullHeight) || $(target).parents().is(fullHeight)) {
		if (e.originalEvent.wheelDelta > 0 || e.originalEvent.detail < 0) {
			// scroll up

			if (st > 0) { // if it has reached the top let it scroll so that the bounce animation on osx/ios is preserved

				pageUp();
				return false;
			}
		} else {
			// scroll down

			if (st < dh - wh) { // if it has reached the bottom let it scroll so that the bounce animation on osx/ios is preserved
				pageDown();
				return false;
			}
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

		$('#change-parent').modal('show').on('shown.bs.modal', function (e) {
			var inkleText = inkle.find('.inkle-text').html();

			$('#change-parent-value').html('"' + inkleText.trim() + '"');
			$('#change-parent-uuid').val(uuid);
			$('#change-parent-page-uuid').val(pageUuid);

		  $('#change-parent-input').focus();
		});

		return false;
	});

	inkle.find('.link-inkle-button').click(function() {
		$('#link-inkle').modal('show');
		$("#link-inkle-uuid").val('#page-'+ pageUuid +'-'+ uuid +'-inkle');
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

	inkle.find('.inkle-textarea').keydown(function (e) {
		if (e.keyCode === 27) {
			inkle.find('.extend-form-switch').show();
			inkle.find('.extend-form-div').slideUp();
		}
	}).keyup(function(e) {

		var value = inkle.find('.inkle-textarea').val();

		if (value == '#') {
			$('#link-inkle').modal('show').on('shown.bs.modal', function (e) {
				var inkleText = inkle.find('.inkle-text').html();

				$('#link-inkle-value').html('"' + inkleText.trim() + '"');
				$('#link-inkle-uuid').val(uuid);
				$('#link-inkle-page-uuid').val(pageUuid);

			  $('#link-inkle-input').focus();
				inkle.find('.inkle-textarea').val('');
			});
		}
	});

	submitOnReturn(inkle.find('.extend-form'));
	submitOnReturn(inkle.find('.edit-form'));

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

function inkleClickActions(element, inkleUuid, wrapper) {
	$(element).click(function() {
		loader();
		jsRoutes.controllers.Inkles.getInkle(inkleUuid).ajax({
			success: function (e) {
				var element = $(e);
				$(wrapper).replaceWith(element);
				fullHeight();

				loader(false);

				window.history.pushState('obj', "Inklin", '/inkles/' + inkleUuid);
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

function inkleLinkClickActions(element, inkleUuid, wrapper) {
	$(element).click(function() {
		loader();
		jsRoutes.controllers.Inkles.getInkle(inkleUuid).ajax({
			success: function (e) {
				$('#view-inkle-modal').modal('show');
				var element = $(e);
				$('#view-inkle-modal-content').html(element);
				fullHeight();

				loader(false);
			},
			error: function() {

				loader(false);
			}
		});
  });

	return false;
}