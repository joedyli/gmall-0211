package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\project-0211\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\project-0211\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "sdfdsf23423fsdlfuLFJEOF#$)@#*@sdf3232");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1OTY1MjQyNzB9.OLlDR7rk4PqgA-LOeSf9KvUilb0SJQGiywhuAdrDZZ-FYi_uyoFKAPHkJkxtK_NGnVMrYSYz5URuoB136ZNHC5j_KhfvVd6KNCE7FOHyYMfxJSA3NIbtrt2qj2DONurZLHGaSePeETQJdFP6XlH03CWJtK3e1n6I1n7Hsijhdfc4CXypWcIZ486OQVIQhJ3bgFSzvcX-grLd2y0kYimsPnEOjnECNXgaX89b9G4wFwZfeNai-PgdU65rTuXBtGPkWLqAO8r57joD2rAyJJPogc3NrCtXte3G-madRkE385AhKXGW6tguOK14T7d0lYduIa92tkJgTmWtvC-qzc3YLA";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
