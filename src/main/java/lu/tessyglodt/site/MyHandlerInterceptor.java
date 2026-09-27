package lu.tessyglodt.site;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lu.tessyglodt.site.service.PageService;

@Component
public class MyHandlerInterceptor implements HandlerInterceptor {

	private final PageService pageService;

	public MyHandlerInterceptor(final PageService pageService) {
		this.pageService = pageService;
	}

	@Override
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
		return true;
	}

	@Override
	public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final ModelAndView modelAndView) throws Exception {
		if (modelAndView != null) {
			modelAndView.addObject("now", LocalDateTime.now());
			// The layout needs "req"; error pages rendered by Spring Boot don't get it from a controller
			if (!modelAndView.getModel().containsKey("req")) {
				modelAndView.addObject("req", request);
			}
			if (request.isUserInRole("ADMIN") && !modelAndView.getViewName().startsWith("redirect:")) {
				modelAndView.addObject("hiddenPages", pageService.getUnpublishedPages());
			}
		}
	}

	@Override
	public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception ex) throws Exception {
		// super.afterCompletion(request, response, handler, ex);
	}

}
