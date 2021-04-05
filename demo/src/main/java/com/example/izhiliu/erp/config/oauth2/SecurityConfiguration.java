package com.izhiliu.erp.config.oauth2;

import com.izhiliu.core.config.oauth2.AbstractSecurityConfiguration;
import com.izhiliu.core.config.oauth2.OAuth2Properties;
import com.izhiliu.core.config.security.AuthoritiesConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;


@Order(value = Integer.MAX_VALUE)
@Configuration
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration extends AbstractSecurityConfiguration {

    public SecurityConfiguration(OAuth2Properties oAuth2Properties) {
        super(oAuth2Properties);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
            .csrf()
                .ignoringAntMatchers("/api/collect/forward/erp")
            .disable()
            .headers()
            .frameOptions()
            .disable()
        .and()
            .cors()
        .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
            .authorizeRequests()
            .antMatchers("/api/**").authenticated()
                .antMatchers("/api/collect/forward/erp").permitAll()
            .antMatchers("/management/health").permitAll()
            .antMatchers("/management/info").permitAll()
            .antMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
            .antMatchers("/swagger-resources/configuration/ui").permitAll();
    }


}
