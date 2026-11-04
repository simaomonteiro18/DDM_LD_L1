package pt.istec.ishopping.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import pt.istec.ishopping.dto.ErroResposta;

@RestControllerAdvice
public class TratadorErros extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TratadorErros.class);

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<Object> tratarNaoAutenticado(NaoAutenticadoException ex, WebRequest request) {
        ErroResposta erro = criarErro(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
        return handleExceptionInternal(ex, erro, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> tratarInesperado(Exception ex, WebRequest request) {
        return handleExceptionInternal(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .collect(Collectors.joining(" "));
        return handleExceptionInternal(ex, criarErro(status, mensagem, request), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("Erro ao processar o pedido", ex);
        }
        Object corpo = body instanceof ErroResposta ? body : criarErro(status, mensagemPara(status), request);
        return super.handleExceptionInternal(ex, corpo, headers, status, request);
    }

    private ErroResposta criarErro(HttpStatusCode status, String mensagem, WebRequest request) {
        String caminho = ((ServletWebRequest) request).getRequest().getRequestURI();
        return ErroResposta.de(status.value(), mensagem, caminho);
    }

    private String mensagemPara(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return "Ocorreu um erro interno. Tente novamente mais tarde.";
        }
        return switch (status.value()) {
            case 404 -> "O recurso pedido não existe.";
            case 405 -> "O método HTTP não é permitido neste endereço.";
            case 406 -> "O formato de resposta pedido não é suportado.";
            case 415 -> "O tipo de conteúdo do pedido não é suportado.";
            default -> "O pedido é inválido.";
        };
    }
}
