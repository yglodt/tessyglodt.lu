package lu.tessyglodt.site.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
public class ConfigWebSecurity {

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.ignoring().requestMatchers("/b/**", "/ckeditor/**", "/css/**", "/fonts/**", "/img/**", "/js/**");
	}

	@Bean
	public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {

		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.anyRequest().permitAll());

		http.formLogin(form -> form
				.loginPage("/login")
				.loginProcessingUrl("/authcheck")
				.usernameParameter("username")
				.passwordParameter("password")
				.failureUrl("/login?error")
				.successHandler((request, response, auth) -> {
					if (request.getServerName().contains("tessyglodt.lu")) {
						response.sendRedirect("https://www.tessyglodt.lu/");
					} else {
						response.sendRedirect("/");
					}
				})
				.permitAll());

		// Matches any HTTP method, so the plain "Log out" link (GET) keeps working
		http.logout(logout -> logout
				.logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/logout"))
				.logoutSuccessUrl("/")
				.permitAll());

		return http.build();
	}

}
