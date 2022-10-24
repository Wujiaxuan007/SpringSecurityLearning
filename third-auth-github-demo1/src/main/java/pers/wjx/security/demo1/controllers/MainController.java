package pers.wjx.security.demo1.controllers;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    /**
     * @param token spring boot 会自动在方法的参数中注入代表用户的 Authentication 对象
     */
    @GetMapping("/")
    public String main(OAuth2AuthenticationToken token) {
        System.out.println(token.getPrincipal());
        return "main.html";
    }
}
