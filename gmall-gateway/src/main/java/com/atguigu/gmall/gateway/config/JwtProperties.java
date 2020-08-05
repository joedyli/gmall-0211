package com.atguigu.gmall.gateway.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "auth.jwt")
@Data
@Slf4j
public class JwtProperties {

    private PublicKey publicKey;

    private String pubKeyPath;
    private String cookieName;

    @PostConstruct
    public void init(){
        try {
            // 把公私钥对应文件的内容读取出来赋值为对应的对象，方便将来使用
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("生成公钥和私钥失败，失败原因：" + e.getMessage());
        }
    }
}
