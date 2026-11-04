package pt.istec.ishopping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ishopping.admin")
public record AdminProperties(String username, String nome, String password) {
}
