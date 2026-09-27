package lu.tessyglodt.site.controller;

import java.io.IOException;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletResponse;

/**
 * A lookup by name/slug that finds nothing (e.g. /page/xyz, /canton/xyz) is a
 * 404, not a server error. sendError() lets Spring Boot render error/404.html.
 */
@ControllerAdvice
public class NotFoundAdvice {

	@ExceptionHandler(EmptyResultDataAccessException.class)
	public void handleNotFound(final HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value());
	}

}
