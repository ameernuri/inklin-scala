
@scrollToElement = (element) ->
	divPosition = $(element).offset()
	$("html, body").animate scrollTop: divPosition.top
	return