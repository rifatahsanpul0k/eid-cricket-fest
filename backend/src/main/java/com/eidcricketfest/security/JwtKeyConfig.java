package com.eidcricketfest.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtKeyConfig {

    @Bean
    RSAPublicKey jwtPublicKey(
            @Value("${app.jwt.public-key}") Resource resource
    ) throws IOException {

        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters.x509().convert(inputStream);
        }
    }

    @Bean
    RSAPrivateKey jwtPrivateKey(
            @Value("${app.jwt.private-key}") Resource resource
    ) throws IOException {

        try (InputStream inputStream = resource.getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(inputStream);
        }
    }

    @Bean
    JwtDecoder jwtDecoder(
            RSAPublicKey publicKey,
            @Value("${app.jwt.issuer}") String issuer
    ) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withPublicKey(publicKey).build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuer)
        );

        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey
    ) {

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();

        JWKSource<SecurityContext> source =
                new ImmutableJWKSet<>(
                        new JWKSet(rsaKey)
                );

        return new NimbusJwtEncoder(source);
    }
}