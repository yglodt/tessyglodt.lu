package lu.tessyglodt.site.spring;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class ConfigWebSecurity {

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.ignoring().requestMatchers("/b/**", "/ckeditor/**", "/css/**", "/fonts/**", "/img/**", "/js/**");
	}

	/*
	 * @Override
	 * public void configure(final WebSecurity web) {
	 * web.ignoring().antMatchers("/b/**", "/ckeditor/**", "/css/**", "/fonts/**", "/img/**", "/js/**");
	 * }
	 */

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		// http.authorizeRequests().and().requiresChannel().antMatchers("/login",
		// "/authcheck", "/admin/**").requiresSecure();

		http.authorizeHttpRequests()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers("/**").permitAll();

		http
				.formLogin()
				.defaultSuccessUrl("/", true)
				.failureUrl("/login?error")
				.loginPage("/login")
				.successHandler(new AuthenticationSuccessHandler() {
					@Override
					public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication auth) throws IOException, ServletException {
						if (request.getServerName().contains("tessyglodt.lu")) {
							response.sendRedirect("https://www.tessyglodt.lu/");
						} else {
							response.sendRedirect("/");
						}
					}
				})
				.loginProcessingUrl("/authcheck").usernameParameter("username").passwordParameter("password")
				.permitAll()
				.and()
				.logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout")).logoutSuccessUrl("/")
				.permitAll();

		http.authorizeHttpRequests().anyRequest().authenticated();

		return http.build();
	}

}
