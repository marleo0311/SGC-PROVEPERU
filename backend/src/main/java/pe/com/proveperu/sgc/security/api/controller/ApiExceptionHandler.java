package pe.com.proveperu.sgc.security.api.controller;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.IntegracionSunatException;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.RechazoSunatException;
import pe.com.proveperu.sgc.cliente.infrastructure.documento.IntegracionDocumentoException;
import pe.com.proveperu.sgc.cliente.application.exception.LimiteConsultaDocumentoException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException exception) {
        return crearProblema(HttpStatus.NOT_FOUND, "Recurso no encontrado", exception.getMessage());
    }

    @ExceptionHandler({ConflictoNegocioException.class, OperacionNoPermitidaException.class})
    ProblemDetail conflicto(RuntimeException exception) {
        return crearProblema(HttpStatus.CONFLICT, "Operación no permitida", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflictoDeIntegridad() {
        return crearProblema(
            HttpStatus.CONFLICT,
            "Conflicto de datos",
            "La operación entra en conflicto con datos existentes"
        );
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaNegocio(ReglaNegocioException exception) {
        return crearProblema(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Regla de negocio",
            exception.getMessage()
        );
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    ProblemDetail solicitudInvalida(SolicitudInvalidaException exception) {
        return crearProblema(HttpStatus.BAD_REQUEST, "Solicitud inválida", exception.getMessage());
    }

    @ExceptionHandler(IntegracionSunatException.class)
    ProblemDetail integracionSunat(IntegracionSunatException exception) {
        return crearProblema(
            HttpStatus.BAD_GATEWAY,
            "Error de integración con SUNAT",
            exception.getMessage()
        );
    }

    @ExceptionHandler(IntegracionDocumentoException.class)
    ProblemDetail integracionDocumento(IntegracionDocumentoException exception) {
        return crearProblema(
            HttpStatus.BAD_GATEWAY,
            "Error al consultar documento",
            exception.getMessage()
        );
    }

    @ExceptionHandler(LimiteConsultaDocumentoException.class)
    ProblemDetail limiteConsultaDocumento(LimiteConsultaDocumentoException exception) {
        return crearProblema(
            HttpStatus.TOO_MANY_REQUESTS,
            "Límite de consultas alcanzado",
            exception.getMessage()
        );
    }

    @ExceptionHandler(RechazoSunatException.class)
    ProblemDetail rechazoSunat(RechazoSunatException exception) {
        ProblemDetail problem = crearProblema(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Comprobante rechazado por SUNAT",
            exception.getMessage()
        );
        problem.setProperty("codigoSunat", exception.getCodigo());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacion(MethodArgumentNotValidException exception) {
        ProblemDetail problem = crearProblema(
            HttpStatus.BAD_REQUEST,
            "Solicitud inválida",
            "Uno o más campos no son válidos"
        );
        Map<String, String> errores = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
            errores.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        problem.setProperty("errores", errores);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail validacionDeParametros(ConstraintViolationException exception) {
        ProblemDetail problem = crearProblema(
            HttpStatus.BAD_REQUEST,
            "Solicitud inválida",
            "Uno o más parámetros no son válidos"
        );
        Map<String, String> errores = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(error ->
            errores.put(error.getPropertyPath().toString(), error.getMessage())
        );
        problem.setProperty("errores", errores);
        return problem;
    }

    private ProblemDetail crearProblema(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
