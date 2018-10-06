package lu.tessyglodt.site.spring;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
//import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lu.tessyglodt.site.MyHandlerInterceptor;

@Configuration
@EnableScheduling
@EnableCaching
@ComponentScan(basePackages = "lu.tessyglodt.site")
public class ConfigWebMvc implements WebMvcConfigurer {

	@Autowired
	private MyHandlerInterceptor myHandlerInterceptor;

	@Override
	public void addInterceptors(final InterceptorRegistry registry) {
		registry.addInterceptor(myHandlerInterceptor).addPathPatterns("/**").excludePathPatterns("/b/**", "/ckeditor/**", "/css/**", "/fonts/**", "/img/**", "/js/**");
	}

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager("page", "accessInfo");
	}

	// @Override
	// public void addResourceHandlers(final ResourceHandlerRegistry registry) {
	// registry.addResourceHandler("/css/**")
	// .addResourceLocations("/css/")
	// .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
	// .resourceChain(false)
	// .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
	// }
	//
	// @Bean
	// @ConditionalOnEnabledResourceChain
	// public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
	// return new ResourceUrlEncodingFilter();
	// }

	// @Bean
	// public EmbeddedServletContainerCustomizer tomcatCustomizer() {
	// return (container) -> {
	// if (container instanceof TomcatEmbeddedServletContainerFactory) {
	// ((TomcatEmbeddedServletContainerFactory) container).addConnectorCustomizers((connector) -> {
	// connector.addUpgradeProtocol(new Http2Protocol());
	// });
	// }
	// };
	// }

	// @Bean
	// public CacheManagerCustomizer<ConcurrentMapCacheManager>
	// cacheManagerCustomizer() {
	// return new CacheManagerCustomizer<ConcurrentMapCacheManager>() {
	// @Override
	// public void customize(ConcurrentMapCacheManager cacheManager) {
	// cacheManager.setCacheNames(Arrays.asList("page", "accessInfo"));
	// }
	// };
	// }

}
