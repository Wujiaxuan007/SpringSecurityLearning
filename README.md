
# Spring Security 使用 Github 作为授权服务器

[![知乎](https://cdn.nlark.com/yuque/0/2022/svg/1233924/1671506271562-235db770-187c-4265-a156-2ac46a13f547.svg)][wjx:zhihu] 

近日去图书馆借了一本书 《Spring Security实战》，主要是看中了它很新且“价值不菲”，一定要借！借了一定要看，看了就是赚到！

书总归要还，只好做些记录和总结，否则回头工作中遇到相关问题，只能拍拍脑袋瓜说 “我记得这个问题的解法在书上看到过，书上说.....书上说啥来着？”
<div align=center>
<img src="https://cdn.nlark.com/yuque/0/2022/png/1233924/1672198728792-6a579858-9b82-4d29-9b52-554f28476fa6.png" width="200px">
</div>

> 本文实现了一个使用了 Spring Boot 和 Spring Security 的 OAuth 2 框架的单点登录( SSO) 应用程序。

> 单点登录应用程序是通过授权服务器 (本文以 Github 为例) 进行身份验证的，然后使用刷新令牌让用户保持登录状态。在本文的示例中，它只代表来自 OAuth2 架构的客户端。

## OAuth 2 授权

常见的 OAuth 2 授权方式：

- 授权码
- 密码
- 刷新令牌
- 客户端凭据

本文使用 Github 作为第三方授权服务器，即授权码授权类型。

### 授权码类型授权步骤
![授权步骤](https://cdn.nlark.com/yuque/0/2022/png/1233924/1672203056471-10849afd-2e35-42a4-a3ef-5a17c4c26c1c.png?x-oss-process=image%2Fresize%2Cw_1500%2Climit_0)

一般授权码授权类型有以下三个步骤：

*1. 使用授权码授权类型发出身份验证请求*

客户端将用户重定向到需要进行身份验证的授权服务器端点。请求查询中使用以下详细信息。

- 带有 code 值的 response_type , 它会告知授权服务器客户端需要一个授权码。客户端需要该授权码用来获取访问令牌。
- client_id 具有客户端 ID 的值，它会标记应用程序本身，
- redirect_uri, 它会告知授权服务器在成功进行身份验证之后将用户重定向到何处。有时授权服务器已经知道每个客户端的默认重定向 URI，就不需要客户端在发送重定向 URI 。
- scope， 被授予的权限
- state，它定义了一个扩展请求伪造（CSRF）令牌，用于 CSRF 防护。

身份验证成功后，授权服务器将根据重定向 URI 回调客户端，并提供授权码和状态值。客户端要检查状态值 是否与它请求中发送的状态值一致，以确认不是其他人视图调用重定向 URI

*2. 使用授权码授权类型获取访问令牌*

客户端将使用步骤 1 返回的授权码再次调用授权服务器以获取令牌。

*3. 使用授权码授权类型调用保护资源*


## 实现单点登录应用
### 1. 注册 OAuth 应用
使用像 Github 这样的第三方授权服务器，意味着应用程序不会管理它的用户，任何人都可以使用 Github 账号登入到我们的应用程序。

与其他授权服务器一样，Github 需要知道它要向那个客户端应用程序发出令牌的。授权类型步骤 1 发送请求会使用**客户端 ID** 和**客户端密钥**。客户端使用这些凭据在授权服务器对自己进行身份验证，因此 OAuth 应用程序必须向 Github 授权服务器进行注册。


[注册链接](https://github.com/settings/applications/new)

![image](https://user-images.githubusercontent.com/41990342/209760574-588f26aa-6fa1-4272-a876-dedb8b2e7b8b.png)

其中Authorization callback URL 即客户端默认回调重定向 URI。“Register application” 成功后将跳转到下图页面，Github 将会提供客户端 ID，点击 “Generate a new client secret” 将生成客户端密钥。
![image](https://user-images.githubusercontent.com/41990342/209760602-676e5f5a-a3a2-47be-a1f5-d03c1bc7d358.png)


### 2. 代码实现

#### 2.1 依赖

pom.xml 添加如下依赖
```xml
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-oauth2-client</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
  </dependencies>
```

#### 2.2 控制器
```java
@Controller
public class MainController {

    /**
     * @param token spring boot 会自动在方法的参数中注入代表用户的 Authentication 对象
     */
    @GetMapping("/")
    public String main(OAuth2AuthenticationToken token) {
        // 打印授权用户信息
        System.out.println(token.getPrincipal());
        return "main.html";
    }
}
```

#### 2.3 main.html
resources/static/main.html
```html

<h1>Hi @MaiHuaDeRen </h1>
```

#### 2.4 配置类

有两种方式实现：

- 注册 ClientRegistration。如果需要一些额外的处理，比如在数据库中存储客户端注册详情或要从 Web 服务中获取它们，就需要创建一个 ClientRegistrationRepository 的自定义实现。见 2.4.1。
- 使用 Spring Boot 配置。对于在内存中使用一个或多个身份验证提供程序所需的一切，就像本文所做的这样，更推荐使用 Spring Boot 配置，比较干净且易于管理。见 2.4.2。

##### 2.4.1 注册 ClientRegistration

[third-auth-github-demo1][file:third-auth-github-demo1]

Spring Security 定义了 ClientRegistration 用来表示 OAuth2 架构中的客户端。构建一个 ClientRegistration 实例需要一些信息，包括：
- 客户端 ID 和密钥
- 用于身份验证的授权类型
- 重定向 URI
- 作用域

如果授权服务器是第三方的，需要从说明文档获取如下配置信息，Github 文档地址：

[Authorizing OAuth Apps - GitHub Docs](docs.github.com/cn/developers/apps/building-oauth-apps/authorizing-oauth-apps)

- 授权 URI —— 客户端将用户重定向到其进行身份验证的 URI
- 令牌 URI —— 客户端为获取访问令牌和刷新令牌而调用的 URI。
- 用户信息URI —— 客户端在获取访问令牌后可以调用的 URI，以获取关于用户的更多详细信息。

```java
private ClientRegistration clientRegistration() {
       ClientRegistration cr = ClientRegistration.withRegistrationId("github")
               .clientId("a7553955a0c534ec5e6b")
               .clientSecret("1795b30b425ebb79e424afa51913f1c724da0dbb")
               .scope(new String[]{"read:user"})
               .authorizationUri("https://github.com/login/oauth/authorize")
               .tokenUri("https://github.com/login/oauth/access_token")
               .userInfoUri("https://api.github.com/user")
               .userNameAttributeName("id")
               .clientName("GitHub")
               .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
               .redirectUriTemplate("{baseUrl}/{action}/oauth2/code/{registrationId}")
               .build();
       return cr;
}
```

可以看出来一些配置是固定值，Spring Security 提供了 CommonOAuth2Provider，里面定义了常见的 ClientRegistration 实例，里面就有 Github。

![image](https://user-images.githubusercontent.com/41990342/209760639-2ecd6f08-3992-4d4d-8806-9303f9f83c48.png)

最终配置类

```java
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
                .clientId("id") // 上文在 Github 注册的 Client ID
                .clientSecret("secret") // 上文在 Github 生成的 Client secrets
                .build();
    }
}
```

##### 2.4.2 Spring Boot 配置

[third-auth-github-demo2][file:third-auth-github-demo2]

```yaml
spring.security.oauth2.client.registration.github.client-id=id
spring.security.oauth2.client.registration.github.client-secret=secret
```

最终配置类

```java
@Configuration
    public class ProjectConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.oauth2Login();

            http.authorizeRequests()
                .anyRequest().authenticated();
        }
    }
}
```

这里不需要指定 `ClientRegistrationRepository` 和 `ClientRegistration`，Spring Boot 会根据 `application.properties` 配置文件自动创建。如果 `CommonOAuth2Provider` 没有对应授权服务的实现，那`application.properties` 还需要配置如下信息，示例：

```yml
spring.security.oauth2.client.provider.{myprovider}.authorization-url=<some uri>
spring.security.oauth2.client.provider.{myprovider}.token-url=<some uri>
```

## 测试
启动应用程序，访问应用程序主页：http://localhost:8080 会重定向以下页面

[sign in to Github](github.com/login?client_id=002e6c84d15becf3560d&return_to=%2Flogin%2Foauth%2Fauthorize%3Fclient_id%3D002e6c84d15becf3560d%26redirect_uri%3Dhttp%253A%252F%252Flocalhost%253A8080%252Flogin%252Foauth2%252Fcode%252Fgithub%26response_type%3Dcode%26scope%3Dread%253Auser%26state%3DcdSI29wLLudI_FGfmcnZNBTVcGKXDsim9_Zu8iibZ5g%253D
)

![image](https://user-images.githubusercontent.com/41990342/209760715-edf74c41-c15a-426e-93b5-4846858b884c.png)

登录成功后，重定向到回应用程序所调用的 URL。下图可以看出来，code 即 Github 提供给应用程序用来请求访问令牌的授权码。


获取令牌是后端调用行为，浏览器是看不出来的，可以相信此时后端应用程序确实获得了一个令牌。还记得吗？我们在控制器中加了日志打印，看下日志吧。

```java
public class MainController {
   /**
	* @param token spring boot 会自动在方法的参数中注入代表用户的 Authentication 对象
	*/
    @GetMapping("/")
    public String main(OAuth2AuthenticationToken token) {
        // 打印授权用户信息
        System.out.println(token.getPrincipal());
        return "main.html";
    }
}
```
日志：

```log
Name: [41990342], Granted Authorities: [[ROLE_USER, SCOPE_read:user]],
User Attributes: [{login=abc, id=123, node_id=MDQ6VXNlcjQxOTkwMzQy, avatar_url=https://avatars.githubusercontent.com/u/41990342?v=4, gravatar_id=, url=https://api.github.com/users/abc, html_url=https://github.com/abc, followers_url=https://api.github.com/users/abc/followers, following_url=https://api.github.com/users/abc/following{/other_user}, gists_url=https://api.github.com/users/abc/gists{/gist_id}, starred_url=https://api.github.com/users/abc/starred{/owner}{/repo}, subscriptions_url=https://api.github.com/users/abc/subscriptions, organizations_url=https://api.github.com/users/abc/orgs, repos_url=https://api.github.com/users/abc/repos, events_url=https://api.github.com/users/abc/events{/privacy}, received_events_url=https://api.github.com/users/abc/received_events, type=User, site_admin=false, name=masheng, company=null, blog=, location=Hangzhou, China, email=null, hireable=null, bio=null, twitter_username=null, public_repos=7, public_gists=0, followers=2, following=3, created_at=2018-08-01T08:57:40Z, updated_at=2022-10-22T10:17:47Z, private_gists=0, total_private_repos=5, owned_private_repos=5, disk_usage=159271, collaborators=0, two_factor_authentication=false, plan={name=free, space=976562499, collaborators=0, private_repos=10000}}]
```
参考
- [《Spring Security实战》](https://book.douban.com/subject/35682757/)


[wjx:zhihu]: https://zhuanlan.zhihu.com/p/576248887

[file:third-auth-github-demo1]: ./third-auth-github-demo1

[file:third-auth-github-demo2]: ./third-auth-github-demo2
