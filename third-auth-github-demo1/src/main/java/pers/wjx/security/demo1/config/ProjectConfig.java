package pers.wjx.security.demo1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class ProjectConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 当调用 oauth2Login 方法时，OAuth2LoginAuthenticationFilter 会拦截请求并应用 OAuth2 身份验证逻辑
        http.oauth2Login(c -> {
            c.clientRegistrationRepository(clientRepository());
        });

        http.authorizeRequests()
                .anyRequest().authenticated();
    }

    /**
     * 用于 OAuth 2.0 / OpenID 客户端注册的存储库，可以有一个或多个 ClientRegistration 对象
     * 我们使用 InMemoryClientRegistrationRepository , 在内存中存储 ClientRegistrations
     */
    private ClientRegistrationRepository clientRepository() {
        var c = clientRegistration();
        return new InMemoryClientRegistrationRepository(c);
    }

    private ClientRegistration clientRegistration() {
        return CommonOAuth2Provider.GITHUB.getBuilder("github")
                .clientId("id")
                .clientSecret("secret")
                .build();
    }
}
