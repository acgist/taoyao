package com.acgist.taoyao.boot.config;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * 文档配置
 * 
 * @author acgist
 */
@Profile("dev")
@Configuration
@ConditionalOnClass(OpenAPI.class)
public class SpringDocAutoConfiguration {

	@Value("${server.port:8888}")
	private Integer port;
	@Autowired
	private TaoyaoProperties taoyaoProperties;

	@Bean
	public GroupedOpenApi liveApi() {
		return GroupedOpenApi.builder()
			.group("live")
			.displayName("直播")
			.packagesToScan("com.acgist.taoyao.live")
			.build();
	}

	@Bean
	public GroupedOpenApi meetingApi() {
		return GroupedOpenApi.builder()
			.group("meeting")
			.displayName("会议")
			.packagesToScan("com.acgist.taoyao.meeting")
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
		// 本地测试不要配置服务器的信息
		return new OpenAPI()
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
			.name("Apache 2.0")
			.url("https://www.apache.org/licenses/LICENSE-2.0.html");
	}

	/**
	 * @return 授权
	 */
	private List<SecurityRequirement> buildSecurity() {
		return List.of(
			new SecurityRequirement()
			.addList(SecurityProperties.BASIC)
		);
	}
	
	/**
	 * @return 授权
	 */
	private Components buildComponents() {
		return new Components()
			.addSecuritySchemes(SecurityProperties.BASIC, this.buildSecurityScheme());
	}
	
	/**
	 * @return 授权
	 */
	private SecurityScheme buildSecurityScheme() {
		return new SecurityScheme()
			.name(SecurityProperties.BASIC)
			.scheme(SecurityProperties.BASIC)
			.in(SecurityScheme.In.HEADER)
			.type(SecurityScheme.Type.HTTP);
	}
	
}
