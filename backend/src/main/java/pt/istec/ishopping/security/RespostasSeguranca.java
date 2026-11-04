package pt.istec.ishopping.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pt.istec.ishopping.dto.ErroResposta;
import tools.jackson.databind.ObjectMapper;

@Component
public class RespostasSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RespostasSeguranca(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escrever(request, response, HttpStatus.UNAUTHORIZED, "Autenticação necessária. O token está em falta, é inválido ou expirou.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN, "Não tem permissão para aceder a este recurso.");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String mensagem)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErroResposta.de(status.value(), mensagem, request.getRequestURI()));
    }
}
