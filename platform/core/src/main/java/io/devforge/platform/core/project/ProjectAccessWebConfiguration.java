package io.devforge.platform.core.project;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProjectAccessWebConfiguration implements WebMvcConfigurer {

    private final ProjectAccessInterceptor projectAccessInterceptor;

    public ProjectAccessWebConfiguration(ProjectAccessInterceptor projectAccessInterceptor) {
        this.projectAccessInterceptor = projectAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(projectAccessInterceptor).addPathPatterns("/api/v1/projects/**");
    }
}
