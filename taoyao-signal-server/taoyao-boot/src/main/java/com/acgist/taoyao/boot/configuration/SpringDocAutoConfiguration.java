package com.acgist.taoyao.boot.configuration;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.acgist.taoyao.boot.config.TaoyaoProperties;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * 文档自动配置
 * 
 * @author acgist
 */
@Profile({ "dev", "sit" })
@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class SpringDocAutoConfiguration {
    
    @Value("${server.port:8888}")
    private Integer port;
    @Value("${taoyao.security.scheme:Basic}")
    private String scheme;
    
    private final TaoyaoProperties taoyaoProperties;
    
	public SpringDocAutoConfiguration(TaoyaoProperties taoyaoProperties) {
        this.taoyaoProperties = taoyaoProperties;
    }
	
	@Bean
	public GroupedOpenApi signalApi() {
		return GroupedOpenApi.builder()
			.group("signal")
			.displayName("信令")
			.packagesToScan("com.acgist.taoyao.signal")
			.build();
	}
	
	@Bean
	public GroupedOpenApi taoyaoApi() {
		return GroupedOpenApi.builder()
			.group("taoyao")
			.displayName("桃夭")
			.packagesToScan("com.acgist.taoyao")
			.build();
	}
	
	@Bean
	@ConditionalOnMissingBean
	public OpenAPI openAPI() {
		return new OpenAPI()
//          .servers(null)
			.info(this.buildInfo())
			.security(this.buildSecurity())
			.components(this.buildComponents());
	}

	/**
	 * @return 基本信息
	 */
	private Info buildInfo() {
		return new Info()
			.contact(this.buildContact())
			.license(this.buildLicense())
			.title(this.taoyaoProperties.getName())
			.version(this.taoyaoProperties.getVersion())
			.description(this.taoyaoProperties.getDescription());
	}
	
	/**
	 * @return 联系方式
	 */
	private Contact buildContact() {
		return new Contact()
			.url(this.taoyaoProperties.getUrl())
			.name(this.taoyaoProperties.getName());
	}

	/**
	 * @return 开源信息
	 */
	private License buildLicense() {
		return new License()
		    .url("https://www.apache.org/licenses/LICENSE-2.0.html")
			.name("Apache 2.0");
	}

	/**
	 * @return 安全授权
	 */
	private List<SecurityRequirement> buildSecurity() {
		return List.of(
			new SecurityRequirement()
			.addList(this.scheme)
		);
	}
	
	/**
	 * @return 安全授权
	 */
	private Components buildComponents() {
		return new Components()
			.addSecuritySchemes(this.scheme, this.buildSecurityScheme());
	}
	
	/**
	 * @return 授权模式
	 */
	private SecurityScheme buildSecurityScheme() {
		return new SecurityScheme()
			.name(this.scheme)
			.scheme(this.scheme)
			.in(SecurityScheme.In.HEADER)
			.type(SecurityScheme.Type.HTTP);
	}
	
}
