
var reachedEnd = false;
var fetching = false;
var page = 1;

function scroller(fetcher) {

	$(window).scroll(function() {

		if((($(document).height() - $(window).scrollTop()) < $(window).height() * 3) && !fetching && !reachedEnd) {

			fetching = true;

			$("#pagination-loader").show();

			$.ajax({
				url: fetcher + page,
				type: "POST",
				success: function(data) {
					$.each(data, function() {
						$.each(this, function(i, a) {
							var stack = Math.floor(Math.random()*10000);

							var inkleBase = "page-" + page + "-stack-" + stack + "-inkle-" + a.id;
							if (a.childrenCount == 1) {
								var inkleCounter = " reply"
							} else {
								var inkleCounter = " replies"
							}

							if (a.boxSecret == true) {
								var secretBox = $("<img>").attr("src", "/assets/images/key.png");
							} else {
								var secretBox = $("<img>").attr("src", "/assets/images/open_box.png")
							}

							$("#inkles-wrapper").append(
								$("<div>").append(
									$("<div>").append(
										"into " + a.boxOwner + "'s ",
										$("<a>").append(
											a.boxName
										).attr("href", "/boxes/" + a.boxId),
										secretBox
									).attr("class", "stack-box"),
									$("<div>").append(
										$("<div>").append(
											$.timeago(new Date(a.createdTime))
										).attr("class", "sibling-date"),
										$("<div>").append(
											$("<span>").append(
												a.inkle
											)
										).attr("class", "top-sibling-content"),
										$("<div>").append(
											$("<b>").append(
												a.childrenCount
											), inkleCounter
										).attr("class", "sibling-count"),
										$("<div>").append(
											$("<a>").append(
												a.inklerUsername
											)
										).attr("class", "sibling-inkler")
									)
									.attr("class", "sibling top-sibling")
									.attr("id", inkleBase + "-top-sibling")
									.attr("parent", a.parentId)
									.attr("self", a.id),
									$("<div>").append(
										$("<img>")
										.attr("src", "/assets/images/bubbler.png")
										.attr("class", "bubbler"),
										$("<div>").append(
											$("<div>").append(
												"write a reply"
											)
											.attr("class", "add-sibling-form-switch")
											.attr("id", inkleBase + "-form-switch"),
											$("<div>").append(
												$("<form>").append(
													$("<textarea>")
													.attr("name", "inkle")
													.attr("placeholder", "how about...")
													.attr("maxlength", "70")
													.attr("id", inkleBase + "-form-textarea"),
													$("<span>").append(
														$("<input>")
														.attr("type", "submit")
														.attr("value", "•••")
													),
													$("<span>").append(
														$("<img>")
														.attr("src", "/assets/images/loader.gif")
													)
													.attr("class", "add-sibling-form-loader hidden")
													.attr("id", inkleBase + "-form-loader")
												)
												.attr("action", "/inkles/extend/" + a.id)
												.attr("method", "POST")
												.attr("id", inkleBase + "-form")
											)
											.attr("class", "add-sibling-form-content")
											.attr("id", inkleBase + "-form-wrapper")
										)
										.attr("class", "add-sibling-form")
										.attr("id", inkleBase + "-extend-form"),
										$("<div>").append(
											$("<div>")
											.attr("id", inkleBase + "-last-pointer")
											.attr("class", "hidden")
										)
										.attr("id", inkleBase + "-siblings")
										.attr("class", "siblings")
									)
									.attr("class", "sibling-wrapper")
									.attr("id", inkleBase + "-sibling-wrapper")
									.attr("stack", stack)
									.attr("level", 0)
								)
								.attr("class", "stack")
								.attr("id", inkleBase + "-stack"),
								'<script>' +
									'submitOnReturn("' + inkleBase + '-form");' +

									'siblingFormSwitch("' + inkleBase + '-form");' +
									'siblingFormHandler(' + page + ', ' + stack + ', ' + a.id + ');' +
									'stackPrepender(' + page + ', ' + stack + ', ' + a.id + ');' +
								'</scr' + 'ipt>'
							),
							$.each(a.children, function() {
								$.each(this, function(b, c) {

									if(c.childrenCount == 1) {
										var childCounter = " reply"
									} else {
										var childCounter = " replies"
									}

									$("#page-" + page + "-stack-" + stack + "-inkle-" + a.id + "-siblings").prepend(
										$("<div>").append(
											$("<div>").append(
												$.timeago(new Date(c.createdTime))
											).attr("class", "sibling-date"),
											$("<div>").append(
												$("<span>").append(
													c.inkle
												)
											)
											.attr("class", "sibling-content page-" + page + "-stack-" + stack + "-inkle-" + a.id + "-sibling")
											.attr("id", "page-" + page + "-stack-" + stack + "-inkle-" + a.id + "-sibling-" + c.id),
											$("<div>").append(
												$("<b>").append(
													c.childrenCount
												), childCounter
											).attr("class", "sibling-count"),
											$("<div>").append(
												$("<a>").append(
													c.inklerUsername
												)
											).attr("class", "sibling-inkler")
										)
										.attr("class", "sibling page-" + page + "-stack-" + stack + "-inkle-" + a.id + "-sibling")
										.attr("id", "page-" + page + "-stack-" + stack + "-inkle-" + a.id + "-sibling-" + c.id),
										'<script>' +
											'$("#page-' + page + '-stack-' + stack + '-inkle-' + a.id + '-sibling-' + c.id + '").click(function() {' +
												'appendStack(' + page + ', ' + stack + ', ' + a.id + ', ' + c.id + ')'+
											'})'+
										'</scr' + 'ipt>'
									);
								});
							});
						});
					});

					page++;
					fetching = false;

					$("#pagination-loader").hide();

					if (data == "") {
						reachedEnd = true;
					}
				}
			});
		}
	})
}

function submitOnReturn(formId) {
	$("#" + formId + "-textarea").keydown(function(e) {
		if (e.keyCode === 13) {
			$("#" + formId).submit();
			return false;
		}
	})
}

function siblingFormSwitch(formId) {
	$("#" + formId + "-switch").click(function() {
		$("#" + formId + "-switch").hide();
		$("#" + formId + "-wrapper").fadeIn();
		$("#" + formId + "-textarea").focus();
	})
}

function newInklePrepender(formId, boxId) {

	$("#" + formId).submit(function() {
		$("#" + formId + "-loader").show();
		$.ajax({
			url: "/inkles/create/" + boxId,
			type: "POST",
			data: $("#" + formId).serialize(),
			success: function(a) {
				var stack = Math.floor(Math.random()*10000);
				var page = 99999;

				var inkleBase = "page-" + page + "-stack-" + stack + "-inkle-" + a.id;
				if (a.childrenCount == 1) {
					var inkleCounter = " reply";
				} else {
					var inkleCounter = " replies";
				}

				if (a.boxSecret == true) {
					var secretBox = $("<img>").attr("src", "/assets/images/key.png");
				} else {
					var secretBox = $("<img>").attr("src", "/assets/images/open_box.png");
				}

				$("#inkles-wrapper").prepend(
					$("<div>").append(
						$("<div>").append(
							"into " + a.boxOwner + "'s ",
							$("<a>").append(
								a.boxName
							).attr("href", "/boxes/" + a.boxId),
							secretBox
						).attr("class", "stack-box"),
						$("<div>").append(
							$("<div>").append(
								$.timeago(new Date(a.createdTime))
							).attr("class", "sibling-date"),
							$("<div>").append(
								$("<span>").append(
									a.inkle
								)
							).attr("class", "top-sibling-content"),
							$("<div>").append(
								$("<b>").append(
									a.childrenCount
								), inkleCounter
							).attr("class", "sibling-count"),
							$("<div>").append(
								$("<a>").append(
									a.inklerUsername
								)
							).attr("class", "sibling-inkler")
						)
						.attr("class", "sibling top-sibling")
						.attr("id", inkleBase + "-top-sibling")
						.attr("parent", a.parentId)
						.attr("self", a.id),
						$("<div>").append(
							$("<img>")
							.attr("src", "/assets/images/bubbler.png")
							.attr("class", "bubbler"),
							$("<div>").append(
								$("<div>").append(
									"write a reply"
								)
								.attr("class", "add-sibling-form-switch")
								.attr("id", inkleBase + "-form-switch"),
								$("<div>").append(
									$("<form>").append(
										$("<textarea>")
										.attr("name", "inkle")
										.attr("placeholder", "how about...")
										.attr("maxlength", "70")
										.attr("id", inkleBase + "-form-textarea"),
										$("<span>").append(
											$("<input>")
											.attr("type", "submit")
											.attr("value", "•••")
										),
										$("<span>").append(
											$("<img>")
											.attr("src", "/assets/images/loader.gif")
										)
										.attr("class", "add-sibling-form-loader hidden")
										.attr("id", inkleBase + "-form-loader")
									)
									.attr("action", "/inkles/extend/" + a.id)
									.attr("method", "POST")
									.attr("id", inkleBase + "-form")
								)
								.attr("class", "add-sibling-form-content")
								.attr("id", inkleBase + "-form-wrapper")
							)
							.attr("class", "add-sibling-form")
							.attr("id", inkleBase + "-extend-form"),
							$("<div>").append(
								$("<div>")
								.attr("id", inkleBase + "-last-pointer")
								.attr("class", "hidden")
							)
							.attr("id", inkleBase + "-siblings")
							.attr("class", "siblings")
						)
						.attr("class", "sibling-wrapper")
						.attr("id", inkleBase + "-sibling-wrapper")
						.attr("stack", stack)
						.attr("level", 0)
					)
					.attr("class", "stack")
					.attr("id", inkleBase + "-stack"),
					'<script>' +
						'submitOnReturn("' + inkleBase + '-form");' +

						'siblingFormSwitch("' + inkleBase + '-form");' +
						'siblingFormHandler(' + page + ', ' + stack + ', ' + a.id + ');' +
						'stackPrepender(' + page + ', ' + stack + ', ' + a.id + ');' +
					'</scr' + 'ipt>'
				);

				$("#" + formId + "-loader").hide();
			}
		});
		return false;
	});
}

function siblingFormHandler(page, stack, inkle) {

	var inkleBase = "page-" + page + "-stack-" + stack + "-inkle-" + inkle;

	$("#" + inkleBase + "-form").submit(function() {

		$("#" + inkleBase + "-form-loader").show();

		$.ajax({
			url: "/inkles/extend/" + inkle,
			type: "POST",
			data: $("#" + inkleBase + "-form").serialize(),
			success: function(data) {

				if (data.childrenCount == 1) {
					var childCounter = " reply"
				} else {
					var childCounter = " replies"
				}

				if (data.inkle != null) {
					$("#" + inkleBase + "-siblings").prepend(
						$("<div>").append(
							$("<div>").append(
								$.timeago(new Date(data.createdTime))
							)
							.attr("class", "sibling-date")
							.attr("title", new Date(data.createdTime)),
							$("<div>").append(
								$("<span>").append(
									data.inkle
								)
							)
							.attr("class", "sibling-content")
							.attr("id", inkleBase + "-sibling-" + data.id + "-content"),
							$("<div>").append(
								$("<b>").append(
									data.childrenCount
								), childCounter
							).attr("class", "sibling-count"),
							$("<div>").append(
								$("<a>").append(
									data.inklerUsername
								).attr("href", "/" + data.inklerUsername)
							).attr("class", "sibling-inkler")
						).attr("class", "sibling " + inkleBase + "-sibling")
						.attr("id", inkleBase + "-sibling-" + data.id),
						'<script>' +
							'$("#' + inkleBase + '-sibling-' + data.id + '").click(function() {' +
								'appendStack(' + page + ', ' + stack + ', ' + inkle + ', ' + data.id + ');' +
							'});' +
						'<\/script>'
					),
					$("#" + inkleBase + "-form-textarea").val("");
				}
				$("#" + inkleBase + "-form-loader").hide();
			}
		})

		return false;
	})
}

function getChildren(page, stack, inkle) {

	var childBase = 'page-' + page + '-stack-' + stack + '-inkle-' + inkle;

	return $.ajax({
		url: "/inkles/get_children/" + inkle,
		type: "POST",
		success: function(data) {
			$.each(data, function() {
				$.each(this, function(i, a) {

					if(a.childrenCount == 1) {
						var childCounter = " reply"
					} else {
						var childCounter = " replies"
					}
					$("#" + childBase + "-siblings").append(
						$("<div>").append(
							$("<div>").append(
								$.timeago(new Date(a.createdTime))
							).attr("class", "sibling-date"),
							$("<div>").append(
								$("<span>").append(
									a.inkle
								)
							)
							.attr("class", "sibling-content " + childBase + "-sibling-content")
							.attr("id", childBase + "-sibling-" + a.id + "-content"),
							$("<div>").append(
								$("<b>").append(
									a.childrenCount
								), childCounter
							).attr("class", "sibling-count"),
							$("<div>").append(
								$("<a>").append(
									a.inklerUsername
								)
							).attr("class", "sibling-inkler")
						)
							.attr("class", "sibling " + childBase + "-sibling")
							.attr("id", childBase + "-sibling-" + a.id)
					).append(
						'<script>' +
							'$("#' + childBase + '-sibling-' + a.id + '").click(function() {' +
								'appendStack(' + page + ', ' + stack + ', ' + inkle + ', ' + a.id + ')' +
							'});' +
						'<\/script>'
					)
				})
			}), $("#" + childBase + "-siblings").append(
				$("<div>").attr("class", "hidden").attr("id", childBase + "-last-pointer")
			)
		}
	});
}

function renderChildren(page, stack, inkle, parent, direction) {
	var parentBase = 'page-' + page + '-stack-' + stack + '-inkle-' + parent;
	var inkleBase = 'page-' + page + '-stack-' + stack + '-inkle-' + inkle;

	var parentLevel = $("#" + inkleBase + "-sibling-wrapper")[0].getAttribute("level");

	if (direction == "up") {
		var wrapperLevel = --parentLevel;

		var ajaxScript =
		'<script>' +

			'$("#' + parentBase + '-siblings-loader").css("display", "table");' +

			'getChildren(' + page + ', ' + stack + ', ' + parent + ').done(function() {' +
				'$("#' + parentBase + '-siblings-loader").hide();' +
				'$("#' + parentBase + '-sibling-wrapper").addClass("collapsed-sibling-wrapper");' +
				'$("#' + parentBase + '-extend-form").hide();' +
				'$(".' + parentBase + '-sibling").hide();' +
				'$("#' + parentBase + '-sibling-' + inkle + '").show().addClass("selected-sibling collapsed-selected-sibling").insertAfter("#' + parentBase + '-last-pointer");' +
				'$("#' + parentBase + '-sibling-' + inkle + '-content").addClass("selected-sibling-content");' +
				'$("#' + parentBase + '-last-pointer").insertAfter("#' + parentBase + '-sibling-' + inkle + '");' +
			'});' +

		'<\/script>';
	} else {
		var wrapperLevel = ++parentLevel;

		var ajaxScript =
		'<script>' +

			'$("#' + parentBase + '-siblings-loader").css("display", "table");' +

			'getChildren(' + page + ', ' + stack + ', ' + parent + ').done(function() {' +
				'$("#' + parentBase + '-siblings-loader").hide();' +
			'});' +

		'<\/script>';
	}

	var fetchedChildren =
	'<div class="sibling-wrapper" id="' + parentBase + '-sibling-wrapper" stack="' + stack + '" level="' + wrapperLevel + '">' +
		'<img class="bubbler" src="/assets/images/bubbler.png">' +

		'<div class="add-sibling-form" id="' + parentBase + '-extend-form">' +
			'<div class="add-sibling-form-switch" id="' + parentBase + '-form-switch">write a reply.</div>' +
			'<div class="add-sibling-form-content" id="' + parentBase + '-form-wrapper">' +
				'<form action="/inkles/extend/' + parent + '" method="POST" id="' + parentBase +'-form">' +
					'<textarea name="inkle" placeholder="how about..." maxlength="70" id="' + parentBase + '-form-textarea"></textarea>' +

					'<span>' +
						'<input type="submit" value="•••" id="' + parentBase + '-form-submit">' +
					'</span>' +

					'<span class="add-sibling-form-loader hidden" id="' + parentBase + '-form-loader">' +
						'<img src="/assets/images/loader.gif">' +
					'</span>' +
				'</form>' +
			'</div>' +
			'<script>' +
				'siblingFormSwitch("' + parentBase + '-form");' +
				'submitOnReturn("' + parentBase + '-form");' +
				'siblingFormHandler(' + page + ', ' + stack + ', ' + parent + ');' +
			'<\/script>' +
		'</div>' +

		'<div class="siblings-loader hidden" id="' + parentBase + '-siblings-loader">' +
			'<span>' +
				'<img src="/assets/images/loader.gif">' +
			'</span>' +
		'</div>' +

		'<div class="siblings" id="' + parentBase + '-siblings">' +
			ajaxScript +
		'</div>' +
	'</div>';

	return fetchedChildren;
}

function appendStack(page, stack, inkle, child) {
	var inkleBase = 'page-' + page + '-stack-' + stack + '-inkle-' + inkle;

	var level = $("#" + inkleBase + "-sibling-wrapper")[0].getAttribute("level");

	$("div[stack=" + stack + "]").each(function() {
		if (this.getAttribute("level") > level*1) {
			this.remove();
		}
	});

	$("#" + inkleBase + "-extend-form").toggle();
	$("." + inkleBase + "-sibling").removeClass("selected-sibling").toggle();
	$("." + inkleBase + "-sibling-content").removeClass("selected-sibling-content");
	$("#" + inkleBase + "-sibling-" + child)
		.addClass("selected-sibling")
		.toggleClass("collapsed-selected-sibling")
		.toggle()
		.insertAfter("#" + inkleBase + "-last-pointer");
	$("#" + inkleBase + "-sibling-" + child + "-content").addClass("selected-sibling-content");
	$("#" + inkleBase + "-last-pointer").insertAfter("#" + inkleBase + "-sibling-" + child);
	$(renderChildren(page, stack, inkle, child, "down")).insertAfter("#" + inkleBase + "-sibling-wrapper");

	$("#" + inkleBase + "-sibling-wrapper").toggleClass("collapsed-sibling-wrapper");


}

function stackPrepender(page, stack, inkle) {

	var inkleBase = "page-" + page + "-stack-" + stack + "-inkle-" + inkle;

	$("#" + inkleBase + "-top-sibling").click(function() {

		var topSiblingParent = $("#" + inkleBase + "-top-sibling")[0].getAttribute("parent");
		var topSiblingSelf = $("#" + inkleBase + "-top-sibling")[0].getAttribute("self");

		if (topSiblingParent != null) {
			$.ajax({
				url: "/inkles/get_parent/" + topSiblingParent,
				type: "POST",
				success: function(data) {

					if (data.childrenCount == 1) {
						var inkleCount = " reply"
					} else {
						var inkleCount = " replies"
					}

					if (data.parentId != null) {
						var hasParent = $("<div>").append(
							$("<img>").attr("src", "/assets/images/has_parent.png")
						).attr("class", "sibling-has-parent")
					}

					$("#" + inkleBase + "-top-sibling")
					.html("")
					.attr("parent", data.parentId)
					.append(
						hasParent,
						$("<div>").append($.timeago(new Date(data.createdTime)))
						.attr("class", "sibling-date"),
						$("<div>").append(
							$("<span>").append(data.inkle)
						)
						.attr("class", "top-sibling-content"),
						$("<div>").append(
							$("<b>").append(data.childrenCount), inkleCount
						)
						.attr("class", "sibling-count"),
						$("<div>").append(
							$("<a>").append(data.inklerUsername)
						)
						.attr("class", "sibling-inkler")
					).attr("self", data.id);

					prependStack(page, stack, inkle, topSiblingSelf, data.id);
				}
			})
		}
	})
}

function prependStack(page, stack, original, inkle, parent) {

	var originalBase = 'page-' + page + '-stack-' + stack + '-inkle-' + original;

  $(renderChildren(page, stack, inkle, parent, "up")).insertAfter("#" + originalBase + "-top-sibling");
}

