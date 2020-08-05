package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "auth.jwt")
@Data
@Slf4j
public class JwtProperties {

    private PublicKey publicKey;
    private PrivateKey privateKey;

    private String pubKeyPath;
    private String priKeyPath;
    private String secret;
    private Integer expire;
    private String cookieName;
    private String unick;

    @PostConstruct
    public void init(){
        try {
            // 初始化公钥和私钥文件，方便判断文件存不存在
            File pubFile = new File(pubKeyPath);
            File priFile = new File(priKeyPath);
            // 如果密码文件不存在，或者不完整，重新生成密钥
            if (!pubFile.exists() || !priFile.exists()){
                RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
            }
            // 把公私钥对应文件的内容读取出来赋值为对应的对象，方便将来使用
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("生成公钥和私钥失败，失败原因：" + e.getMessage());
        }
    }
}
