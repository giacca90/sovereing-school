package com.sovereingschool.back_base.Controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sovereingschool.back_base.Interfaces.IInitAppService;

@RestController
@PreAuthorize("hasAnyRole('GUEST', 'USER', 'PROF', 'ADMIN')")
@RequestMapping("/init")
public class InitController {

    private static final Logger logger = LoggerFactory.getLogger(InitController.class);

    private IInitAppService initAppService;

    /**
     * Constructor de InitController
     *
     * @param initAppService Servicio de inicialización
     */
    public InitController(IInitAppService initAppService) {
        this.initAppService = initAppService;
    }

    /**
     * Función para obtener la información inicial
     * 
     * @return ResponseEntity<Object> con el resultado de la operación
     */
    @GetMapping()
    public ResponseEntity<?> getInit() {
        Object response = new Object();
        try {
            response = this.initAppService.getInit();
            String initToken = this.initAppService.getInitToken();
            // Construir la cookie segura
            ResponseCookie initTokenCookie = ResponseCookie.from("initToken", initToken)
                    .httpOnly(true) // No accesible desde JavaScript
                    .secure(true) // Solo por HTTPS
                    .path("/") // Ruta donde será accesible
                    .sameSite("None") // Cambia a "None" si trabajas con frontend separado
                    .build();

            return ResponseEntity.ok()
                    .header("Set-Cookie", initTokenCookie.toString())
                    .body(response);
        } catch (Exception e) {
            logger.error("Error en la carga inicial: " + e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Función para obtener el token de inicialización
     * 
     * @return ResponseEntity<String> con el resultado de la operación
     */
    @GetMapping("/auth")
    public ResponseEntity<?> auth(@CookieValue(required = false) String refreshToken) {
        String initToken = this.initAppService.getInitToken();
        ResponseCookie initTokenCookie = ResponseCookie.from("initToken", initToken)
                .httpOnly(true) // No accesible desde JavaScript
                .secure(true) // Solo por HTTPS
                .path("/") // Ruta donde será accesible
                .sameSite("None") // Cambia a "None" si trabajas con frontend separado
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", initTokenCookie.toString())
                .body("OK");
    }
}
