package com.sovereingschool.back_base.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.sovereingschool.back_base.Configurations.WebClientConfig;
import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.CursosUsuario;
import com.sovereingschool.back_base.Interfaces.IUsuarioService;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Exceptions.InternalComunicationException;
import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Login;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.LoginRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class UsuarioService implements IUsuarioService {

    /**
     * Función para generar un color hexadecimal aleatorio para el usuario sin foto
     * 
     * @return String con el color hexadecimal
     */
    private static Random random = new Random();

    private static String generarColorHex() {
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepo;
    private final CursoRepository cursoRepo;
    private final LoginRepository loginRepo;
    private final JwtUtil jwtUtil;
    private final WebClientConfig webClientConfig;
    private final JavaMailSender mailSender;
    private final InitAppService initAppService;
    private final SpringTemplateEngine templateEngine;

    protected final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final String backChatURL;
    private final String backStreamURL;
    private final String frontURL;
    private final String uploadDir;

    /**
     * Constructor de UsuarioService
     *
     * @param passwordEncoder PasswordEncoder
     * @param usuarioRepo     Repositorio de usuarios
     * @param cursoRepo       Repositorio de cursos
     * @param loginRepo       Repositorio de logins
     * @param jwtUtil         Utilidad de JWT
     * @param webClientConfig Configuración de WebClient
     * @param mailSender      Envío de correos
     * @param initAppService  Servicio de inicialización
     * @param templateEngine  Motor de plantillas
     * @param backChatURL     URL del microservicio de chat
     * @param backStreamURL   URL del microservicio de streaming
     * @param frontURL        URL del microservicio de front
     * @param uploadDir       Ruta de carga de archivos
     */
    public UsuarioService(
            PasswordEncoder passwordEncoder,
            UsuarioRepository usuarioRepo,
            CursoRepository cursoRepo,
            LoginRepository loginRepo,
            JwtUtil jwtUtil,
            WebClientConfig webClientConfig,
            JavaMailSender mailSender,
            InitAppService initAppService,
            SpringTemplateEngine templateEngine,
            @Value("${variable.BACK_CHAT_DOCKER}") String backChatURL,
            @Value("${variable.BACK_STREAM_DOCKER}") String backStreamURL,
            @Value("${variable.FRONT}") String frontURL,
            @Value("${variable.FOTOS_DIR}") String uploadDir) {

        this.passwordEncoder = passwordEncoder;
        this.usuarioRepo = usuarioRepo;
        this.cursoRepo = cursoRepo;
        this.loginRepo = loginRepo;
        this.jwtUtil = jwtUtil;
        this.webClientConfig = webClientConfig;
        this.mailSender = mailSender;
        this.initAppService = initAppService;
        this.templateEngine = templateEngine;

        this.backChatURL = backChatURL;
        this.backStreamURL = backStreamURL;
        this.frontURL = frontURL;
        this.uploadDir = uploadDir;
    }

    /**
     * Función para crear un nuevo usuario
     * 
     * @param new_usuario Objeto NewUsuario con los datos del usuario
     * @return Objeto AuthResponse con los datos del usuario
     * @throws RepositoryException
     * @throws InternalComunicationException
     * @throws DataIntegrityViolationException si el usuario ya existe
     * @throws IOException                     si ocurre un error al subir la foto
     * @throws MessagingException              si ocurre un error al enviar el
     *                                         correo
     * @throws RuntimeException                si ocurre un error en el servidor
     * 
     */
    @Override
    public AuthResponse createUsuario(NewUsuario newUsuario) throws RepositoryException, InternalComunicationException {
        Usuario usuario = new Usuario(
                null, // Long id_usuario
                newUsuario.nombreUsuario(), // String nombre_usuario
                newUsuario.fotoUsuario() == null || newUsuario.fotoUsuario().isEmpty()
                        ? new ArrayList<>(Arrays.asList(generarColorHex()))
                        : newUsuario.fotoUsuario(), // List<String> foto_usuario
                null, // Strting presentación
                RoleEnum.USER, // Integer rol_usuario
                newUsuario.planUsuario(), // Plan plan_usuario
                newUsuario.cursosUsuario(), // List<String> cursos_usuario
                newUsuario.fechaRegistroUsuario(), // Date fecha_registro_usuario
                true,
                true,
                true,
                true);
        try {
            Usuario usuarioInsertado = this.usuarioRepo.save(usuario);
            if (usuarioInsertado.getIdUsuario() == null) {
                throw new RepositoryException("Error al crear el usuario");
            }
            Login login = new Login();
            login.setUsuario(usuarioInsertado);
            login.setCorreoElectronico(newUsuario.correoElectronico());
            login.setPassword(passwordEncoder.encode(newUsuario.password()));
            this.loginRepo.save(login);

            // Creamos la respuesta con JWT
            List<SimpleGrantedAuthority> roles = new ArrayList<>();
            roles.add(new SimpleGrantedAuthority("ROLE_" + usuarioInsertado.getRollUsuario().name()));
            UserDetails userDetails = new User(usuarioInsertado.getNombreUsuario(),
                    login.getPassword(),
                    usuarioInsertado.getIsEnabled(),
                    usuarioInsertado.getAccountNoExpired(),
                    usuarioInsertado.getCredentialsNoExpired(),
                    usuarioInsertado.getAccountNoLocked(),
                    roles);

            Authentication auth = new UsernamePasswordAuthenticationToken(newUsuario.correoElectronico(),
                    userDetails.getPassword(),
                    userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);
            String accessToken = jwtUtil.generateToken(auth, "access", usuarioInsertado.getIdUsuario());
            String refreshToken = jwtUtil.generateToken(auth, "refresh", usuarioInsertado.getIdUsuario());

            // Actualizar el SSR
            this.updateSSR();
            return new AuthResponse(true, "Usuario creado con éxito", usuarioInsertado, accessToken, refreshToken);

        } catch (DataIntegrityViolationException e) {
            logger.error("El usuario ya existe");
            throw new DataIntegrityViolationException("El usuario ya existe");
        }
    }

    /**
     * Función para obtener los datos del usuario
     * 
     * @param id_usuario ID del usuario
     * @return Objeto Usuario con los datos del usuario
     * @throws EntityNotFoundException si el usuario no existe
     * 
     */
    @Override
    public Usuario getUsuario(Long idUsuario) {
        return this.usuarioRepo.findUsuarioForId(idUsuario).orElseThrow(() -> {
            logger.error("Error en obtener el usuario con ID {}", idUsuario);
            return new EntityNotFoundException("Error en obtener el usuario con ID " + idUsuario);
        });
    }

    /**
     * Función para obtener el nombre del usuario
     * 
     * @param id_usuario ID del usuario
     * @return String con el nombre del usuario
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     * 
     */
    @Override
    public String getNombreUsuario(Long idUsuario) {
        return this.usuarioRepo.findNombreUsuarioForId(idUsuario).orElseThrow(() -> {
            logger.error("Error en obtener el nombre del usuario con ID {}", idUsuario);
            return new EntityNotFoundException("Error en obtener el nombre del usuario con ID " + idUsuario);
        });
    }

    /**
     * Función para obtener las fotos del usuario
     * 
     * @param id_usuario ID del usuario
     * @return Lista de String con las fotos del usuario
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     * 
     */
    @Override
    public List<String> getFotosUsuario(Long idUsuario) {
        return this.usuarioRepo.findUsuarioForId(idUsuario)
                .map(Usuario::getFotoUsuario)
                .orElse(null);
    }

    /**
     * Función para obtener el rol del usuario
     * 
     * @param id_usuario ID del usuario
     * @return RoleEnum con el rol del usuario
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     *
     */
    @Override
    public RoleEnum getRollUsuario(Long idUsuario) {
        return this.usuarioRepo.findRollUsuarioForId(idUsuario).orElseThrow(() -> {
            logger.error("Error en obtener el rol del usuario con ID {}", idUsuario);
            return new EntityNotFoundException("Error en obtener el rol del usuario con ID " + idUsuario);
        });
    }

    /**
     * Función para obtener el plan del usuario
     * 
     * @param id_usuario ID del usuario
     * @return Plan con el plan del usuario
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     *
     */
    @Override
    public Plan getPlanUsuario(Long idUsuario) {
        return this.usuarioRepo.findPlanUsuarioForId(idUsuario).orElseThrow(() -> {
            logger.error("Error en obtener el plan del usuario con ID {}", idUsuario);
            return new EntityNotFoundException("Error en obtener el plan del usuario con ID " + idUsuario);
        });
    }

    /**
     * Función para obtener los cursos del usuario
     * 
     * @param id_usuario ID del usuario
     * @return Lista de Curso con los cursos del usuario
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     * @throws IllegalStateException    si el usuario no está autenticado
     * @throws AccessDeniedException    si el usuario no tiene permiso para acceder
     *                                  a este recurso
     * 
     */
    @Override
    public List<Curso> getCursosUsuario(Long idUsuario) {
        return this.usuarioRepo.findUsuarioForId(idUsuario)
                .map(Usuario::getCursosUsuario)
                .orElse(null);
    }

    /**
     * Función para actualizar un usuario
     * 
     * @param usuario Objeto Usuario con los datos del usuario
     * @return Usuario con los datos actualizados
     * @throws InternalServerException
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     * @throws IllegalStateException    si el usuario no está autenticado
     * @throws AccessDeniedException    si el usuario no tiene permiso para acceder
     *                                  a este recurso
     *
     */
    @Override
    public Usuario updateUsuario(Usuario usuario) throws InternalServerException {
        Usuario usuarioOld = this.getUsuario(usuario.getIdUsuario());

        for (String foto : usuarioOld.getFotoUsuario()) {
            if (!usuario.getFotoUsuario().contains(foto)) {
                Path photoPath = null;
                if (foto.contains("/")) {
                    photoPath = Paths.get(uploadDir, foto.substring(foto.lastIndexOf("/") + 1));
                } else {
                    photoPath = Paths.get(uploadDir, foto);
                }
                try {
                    if (Files.exists(photoPath)) {
                        Files.delete(photoPath);
                    } else {
                        logger.error("La foto no existe: {}", photoPath.toString());
                    }
                } catch (IOException e) {
                    throw new InternalServerException(
                            "Error al eliminar la foto: " + photoPath.toString() + ": " + e.getMessage());
                }
            }
        }

        return this.usuarioRepo.save(usuario);
    }

    /**
     * Función para cambiar el plan del usuario
     * 
     * @param usuario Objeto Usuario con los datos del usuario
     * @return Integer con el resultado de la operación
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     * @throws IllegalStateException    si el usuario no está autenticado
     * @throws AccessDeniedException    si el usuario no tiene permiso para acceder
     *                                  a este recurso
     * 
     */
    @Override
    public Integer changePlanUsuario(Usuario usuario) {
        return this.usuarioRepo.changePlanUsuarioForId(usuario.getIdUsuario(), usuario.getPlanUsuario())
                .orElseThrow(() -> {
                    logger.error("Error en cambiar el plan del usuario");
                    return new EntityNotFoundException("Error en cambiar el plan del usuario");
                });
    }

    /**
     * Función para cambiar los cursos del usuario
     * 
     * @param usuario Objeto Usuario con los datos del usuario
     * @return Integer con el resultado de la operación
     * @throws RepositoryException
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     * @throws IllegalStateException    si el usuario no está autenticado
     * @throws AccessDeniedException    si el usuario no tiene permiso para acceder
     *                                  a este recurso
     *
     */
    @Override
    public Integer changeCursosUsuario(CursosUsuario cursosUsuario) throws RepositoryException {
        Usuario usuario = this.usuarioRepo.findUsuarioForId(cursosUsuario.idUsuario()).orElseThrow(() -> {
            logger.error("Error en obtener el usuario con ID {}", cursosUsuario.idUsuario());
            return new EntityNotFoundException("Error en obtener el usuario con ID " + cursosUsuario.idUsuario());
        });
        List<Curso> cursos = this.cursoRepo.findAllById(cursosUsuario.idsCursos());
        usuario.setCursosUsuario(cursos);

        try {
            // Añadir el usuario al microservicio de stream
            this.createUsuarioStream(usuario);

            // Añadir el usuario al microservicio de chat
            this.createUsuarioChat(usuario);
        } catch (Exception e) {
            logger.error("Error en cambiar los cursos del usuario: {}", e.getMessage());
        }

        return this.usuarioRepo.changeUsuarioForId(cursosUsuario.idUsuario(), usuario).orElseThrow(() -> {
            logger.error("Error en cambiar los cursos del usuario");
            return new RepositoryException("Error en cambiar los cursos del usuario");
        });
    }

    /**
     * Función para eliminar un usuario
     * 
     * @param id ID del usuario
     * @return String con el resultado de la operación
     * @throws RepositoryException
     * @throws InternalComunicationException
     * @throws EntityNotFoundException       si el usuario no existe
     * @throws RuntimeException              si ocurre un error en el servidor
     * @throws IllegalArgumentException      si el ID no es válido
     * 
     */
    @Override
    public String deleteUsuario(Long id) throws RepositoryException, InternalComunicationException {
        this.usuarioRepo.findUsuarioForId(id).orElseThrow(() -> {
            logger.error("Error en obtener el usuario con ID {}", id);
            return new EntityNotFoundException("Error en obtener el usuario con ID " + id);
        });
        try {
            this.loginRepo.deleteById(id);
            this.usuarioRepo.deleteById(id);

            // Actualizar el SSR
            this.initAppService.refreshSSR();
        } catch (IllegalArgumentException e) {
            logger.error("Error en eliminar el usuario con ID {}", id);
            throw new IllegalArgumentException("Error en eliminar el usuario con ID " + id);
        } catch (InternalComunicationException e) {
            logger.error("Error en actualizar el SSR: {}", e.getMessage());
        }

        try {

            // Eliminamos el usuario del chat
            this.deleteUsuarioChat(id);

            // Eliminamos el usuario del stream
            this.deleteUsuarioStream(id);
        } catch (Exception e) {
            logger.error("Error interno al eliminar el usuario: ", e);
        }

        return "Usuario eliminado con éxito!!!";
    }

    /**
     * Función para obtener los profesores
     * 
     * @return Lista de Usuarios
     */
    @Override
    public List<Usuario> getProfes() {
        return this.usuarioRepo.findProfes();
    }

    /**
     * Función para enviar el correo de confirmación
     * 
     * @param newUsuario Objeto NewUsuario con los datos del usuario
     * @return Boolean con el resultado de la operación
     * @throws MessagingException       si ocurre un error al enviar el
     *                                  correo
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     * 
     */
    @Override
    public boolean sendConfirmationEmail(NewUsuario newUsuario) throws InternalServerException {
        Context context = new Context();
        String token = jwtUtil.generateRegistrationToken(newUsuario);
        context.setVariable("nombre", newUsuario.nombreUsuario());
        context.setVariable("link", frontURL + "/confirm-email?token=" + token);
        context.setVariable("currentYear", Year.now().getValue());

        String htmlContent = templateEngine.process("mail-registro", context);

        // Enviar el correo como HTML
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(newUsuario.correoElectronico());
            helper.setSubject("Confirmación de Correo Electrónico");
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            return true;
        } catch (MailAuthenticationException e) {
            // Error de autenticación con el servidor SMTP
            throw new InternalServerException("Error de autenticación al enviar el correo: " + e.getMessage());
        } catch (MailSendException e) {
            // Error al enviar el mensaje
            throw new InternalServerException("Error al enviar el correo: " + e.getMessage());
        } catch (MailException e) {
            // Otros errores relacionados con el envío de correos
            throw new InternalServerException("Error general al enviar el correo: " + e.getMessage());
        } catch (Exception e) {
            // Cualquier otro error inesperado
            throw new InternalServerException("Error inesperado al enviar el correo: " + e.getMessage());
        }
    }

    /**
     * Función para obtener todos los usuarios
     * 
     * @return Lista de Usuarios
     * @throws RepositoryException
     */
    @Override
    public List<Usuario> getAllUsuarios() throws RepositoryException {
        return this.usuarioRepo.findAll();
    }

    /**
     * Función para crear el chat del usuario
     * Se llama cuando un usuario adquiere un curso
     * 
     * TODO: Cambiar por Redis Stream
     * 
     * @param usuario Usuario a crear el chat
     * @throws InternalComunicationException
     * 
     */
    protected void createUsuarioChat(Usuario usuario) throws InternalComunicationException {
        try {
            WebClient webClient = webClientConfig.createSecureWebClient(backChatURL);
            webClient.post()
                    .uri("/crea_usuario_chat")
                    .body(Mono.just(usuario), Usuario.class)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del microservicio de stream: {}", errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio de chat: {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        // Maneja el resultado cuando esté disponible
                        if (res == null || !res.equals("Usuario chat creado con exito!!!")) {
                            logger.error("Error en crear el usuario en el chat: ");
                            logger.error(res);
                        }
                    });
        } catch (Exception e) {
            throw new InternalComunicationException("Error en crear el usuario en el chat: " + e.getMessage());
        }
    }

    /**
     * Función para crear el streaming del usuario
     * 
     * TODO: Cambiar por Redis Stream
     * 
     * @param usuarioInsertado Usuario a crear el streaming
     * @throws InternalComunicationException
     * 
     */
    protected void createUsuarioStream(Usuario usuario) throws InternalComunicationException {
        try {
            WebClient webClientStream = webClientConfig.createSecureWebClient(backStreamURL);
            webClientStream.put()
                    .uri("/nuevoUsuario")
                    .body(Mono.just(usuario), Usuario.class)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del microservicio de stream: {}", errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Excepción al conectar con el microservicio de stream: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .subscribe(res -> {
                        if (res == null || !res.equals("Nuevo Usuario Insertado con Exito!!!")) {
                            logger.error("Error inesperado al crear el usuario en el stream:");
                            logger.error(res);
                        }
                    });

        } catch (Exception e) {
            throw new InternalComunicationException("Error en crear el usuario en el stream: " + e.getMessage());
        }
    }

    /**
     * Función para actualizar el SSR
     * 
     * @throws InternalComunicationException
     */
    protected void updateSSR() throws InternalComunicationException {
        try {
            this.initAppService.refreshSSR();
        } catch (Exception e) {
            throw new InternalComunicationException("Error en actualizar el SSR: " + e.getMessage());
        }
    }

    /**
     * Función para eliminar el streaming del usuario
     * 
     * TODO: Cambiar por Redis Stream
     * 
     * @param id ID del usuario
     * @throws InternalComunicationException
     */
    protected void deleteUsuarioStream(Long id) throws InternalComunicationException {
        try {
            WebClient webClientStream = webClientConfig.createSecureWebClient(backStreamURL);
            webClientStream.delete().uri("/deleteUsuarioStream/" + id)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del microservicio de stream: {}", errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio de stream: {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        // Maneja el resultado cuando esté disponible
                        if (res == null || !res.equals("Usuario stream borrado con exito!!!")) {
                            logger.error("Error en borrar el usuario del stream");
                            logger.error(res);
                        }
                    });
        } catch (Exception e) {
            throw new InternalComunicationException("Error en eliminar el usuario del stream: " + e.getMessage());
        }
    }

    /**
     * Función para eliminar el chat del usuario
     * 
     * @param id ID del usuario
     * @throws InternalComunicationException
     * 
     *                                       TODO: Cambiar por Redis Stream
     */
    protected void deleteUsuarioChat(Long id) throws InternalComunicationException {
        try {
            WebClient webClientStream = webClientConfig.createSecureWebClient(backChatURL);
            webClientStream.delete().uri("/delete_usuario_chat/" + id)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                logger.error("Error HTTP del microservicio de stream: {}", errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        logger.error("Error al conectar con el microservicio de stream: {}", e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        // Maneja el resultado cuando esté disponible
                        if (res == null || !res.equals("Usuario chat borrado con exito!!!")) {
                            logger.error("Error en borrar el chat del usuario");
                            logger.error(res);
                        }
                    });
        } catch (Exception e) {
            throw new InternalComunicationException("Error en borrar el chat del usuario: " + e.getMessage());
        }
    }

}
