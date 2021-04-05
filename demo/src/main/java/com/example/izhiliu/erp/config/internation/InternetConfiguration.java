package com.izhiliu.erp.config.internation;

import com.izhiliu.core.config.internation.InternationInterceptor;
import com.izhiliu.erp.config.converter.LocalProductStatusEnumConverter;
import io.github.jhipster.config.locale.AngularCookieLocaleResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author: louis
 * @Date: 2019/6/25 14:09
 */
@Configuration
@Component
public class InternetConfiguration implements WebMvcConfigurer {
    @Autowired
    private InternationInterceptor internationInterceptor;

    @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        AngularCookieLocaleResolver cookieLocaleResolver = new AngularCookieLocaleResolver();
        cookieLocaleResolver.setCookieName("NG_TRANSLATE_LANG_KEY");
        return cookieLocaleResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internationInterceptor).addPathPatterns("/**");
    }
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(registry);
        registry.addConverter(new LocalProductStatusEnumConverter());
    }
}
