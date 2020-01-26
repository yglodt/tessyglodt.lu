package lu.tessyglodt.site.spring;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.servlet.support.csrf.CsrfRequestDataValueProcessor;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

@Configuration
@EnableWebSecurity
public class ConfigWebSecurity extends WebSecurityConfigurerAdapter {

	@Override
	public void configure(final WebSecurity web) {
		web.ignoring().antMatchers("/b/**", "/ckeditor/**", "/css/**", "/fonts/**", "/img/**", "/js/**");
	}


	@Override
	protected void configure(final HttpSecurity http) throws Exception {

		// http.authorizeRequests().and().requiresChannel().antMatchers("/login",
		// "/authcheck", "/admin/**").requiresSecure();

		http.authorizeRequests()
				.antMatchers("/admin/**").hasRole("ADMIN")
				.antMatchers("/**").permitAll();

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

		http
				.authorizeRequests().anyRequest().authenticated();
		
		http.csrf();
	}

}
