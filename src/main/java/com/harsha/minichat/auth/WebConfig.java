package com.harsha.minichat.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private static final String SCHEME = "bearerToken";

	private final AuthInterceptor authInterceptor;

	public WebConfig(AuthInterceptor authInterceptor) {
		this.authInterceptor = authInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(authInterceptor).addPathPatterns("/api/v4/**");
	}

	/** Adds the "Authorize" button to Swagger UI so the login token can be pasted once. */
	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Mini chat backend")
						.version("0.0.1")
						.description("Rebuilt from an API blueprint that REA recorded from a running chat app. "
								+ "Log in, copy the 'Token' response header, then click Authorize."))
				.components(new Components().addSecuritySchemes(SCHEME,
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
				.addSecurityItem(new SecurityRequirement().addList(SCHEME));
	}
}
