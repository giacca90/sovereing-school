package com.sovereingschool.back_base.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
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

import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.CursosUsuario;
import com.sovereingschool.back_base.Interfaces.IUsuarioService;
import com.sovereingschool.back_common.DTOs.NewUsuario;
import com.sovereingschool.back_common.Models.Curso;
import com.sovereingschool.back_common.Models.Login;
import com.sovereingschool.back_common.Models.Plan;
import com.sovereingschool.back_common.Models.RoleEnum;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.CursoRepository;
import com.sovereingschool.back_common.Repositories.LoginRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
@Transactional
public class UsuarioService implements IUsuarioService {

    public static String generarColorHex() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private final PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private CursoRepository cursoRepo;

    @Autowired
    private LoginRepository loginRepo;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${variable.BACK_CHAT}")
    private String backChatURL;

    @Value("${variable.BACK_STREAM}")
    private String backStreamURL;

    @Value("${variable.FRONT}")
    private String frontURL;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${variable.FOTOS_DIR}")
    private String uploadDir;

    @Autowired
    private SpringTemplateEngine templateEngine;;

    UsuarioService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Función para crear un nuevo usuario
     * 
     * @param new_usuario Objeto NewUsuario con los datos del usuario
     * @return Objeto AuthResponse con los datos del usuario
     * @throws DataIntegrityViolationException si el usuario ya existe
     * @throws IOException                     si ocurre un error al subir la foto
     * @throws MessagingException              si ocurre un error al enviar el
     *                                         correo
     * @throws RuntimeException                si ocurre un error en el servidor
     * 
     */
    @Override
    public AuthResponse createUsuario(NewUsuario new_usuario) {
        Usuario usuario = new Usuario(
                null, // Long id_usuario
                new_usuario.getNombre_usuario(), // String nombre_usuario
                new_usuario.getFoto_usuario() == null || new_usuario.getFoto_usuario().isEmpty()
                        ? new ArrayList<>(Arrays.asList(generarColorHex()))
                        : new_usuario.getFoto_usuario(), // List<String> foto_usuario
                null, // Strting presentación
                RoleEnum.USER, // Integer rol_usuario
                new_usuario.getPlan_usuario(), // Plan plan_usuario
                new_usuario.getCursos_usuario(), // List<String> cursos_usuario
                new_usuario.getFecha_registro_usuario(), // Date fecha_registro_usuario
                true,
                true,
                true,
                true);
        try {
            Usuario usuarioInsertado = this.usuarioRepo.save(usuario);
            if (usuarioInsertado.getId_usuario() == null) {
                throw new RuntimeException("Error al crear el usuario");
            }
            Login login = new Login();
            login.setUsuario(usuarioInsertado);
            login.setCorreo_electronico(new_usuario.getCorreo_electronico());
            login.setPassword(passwordEncoder.encode(new_usuario.getPassword()));
            this.loginRepo.save(login);

            // Crear el usuario en el microservicio de chat
            try {
                WebClient webClient = createSecureWebClient(backChatURL);
                webClient.post().uri("/crea_usuario_chat")
                        .body(Mono.just(usuarioInsertado), Usuario.class)
                        .retrieve()
                        .onStatus(
                                status -> status.isError(),
                                response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                    System.err.println("Error HTTP del microservicio de stream: " + errorBody);
                                    return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                                }))
                        .bodyToMono(String.class)
                        .onErrorResume(e -> {
                            System.err.println("Error al conectar con el microservicio de chat: " + e.getMessage());
                            return Mono.empty(); // Continuar sin interrumpir la aplicación
                        }).subscribe(res -> {
                            // Maneja el resultado cuando esté disponible
                            if (res == null || !res.equals("Usuario chat creado con exito!!!")) {
                                System.err.println("Error en crear el usuario en el chat: ");
                                System.err.println(res);
                            }
                        });
            } catch (Exception e) {
                System.err.println("Error en crear el usuario en el chat: " + e.getMessage());
            }

            // Crear el usuario en el microservicio de stream
            try {
                WebClient webClientStream = createSecureWebClient(backStreamURL);
                webClientStream.put()
                        .uri("/nuevoUsuario")
                        .body(Mono.just(usuarioInsertado), Usuario.class)
                        .retrieve()
                        .onStatus(
                                status -> status.isError(), // compatible con HttpStatusCode
                                response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                    System.err.println("Error HTTP del microservicio de stream: " + errorBody);
                                    return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                                }))
                        .bodyToMono(String.class)
                        .onErrorResume(e -> {
                            System.err
                                    .println("Excepción al conectar con el microservicio de stream: " + e.getMessage());
                            return Mono.empty();
                        })
                        .subscribe(res -> {
                            if (res == null || !res.equals("Nuevo Usuario Insertado con Exito!!!")) {
                                System.err.println("Error inesperado al crear el usuario en el stream:");
                                System.err.println(res);
                            }
                        });

            } catch (Exception e) {
                System.err.println("Error en crear el usuario en el stream: " + e.getMessage());
            }

            // Creamos la respuesta con JWT
            List<SimpleGrantedAuthority> roles = new ArrayList<>();
            roles.add(new SimpleGrantedAuthority("ROLE_" + usuarioInsertado.getRoll_usuario().name()));
            UserDetails userDetails = new User(usuarioInsertado.getNombre_usuario(),
                    login.getPassword(),
                    usuarioInsertado.getIsEnabled(),
                    usuarioInsertado.getAccountNoExpired(),
                    usuarioInsertado.getCredentialsNoExpired(),
                    usuarioInsertado.getAccountNoLocked(),
                    roles);

            Authentication auth = new UsernamePasswordAuthenticationToken(new_usuario.getCorreo_electronico(),
                    userDetails.getPassword(),
                    userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);
            String accessToken = jwtUtil.generateToken(auth, "access", usuarioInsertado.getId_usuario());
            String refreshToken = jwtUtil.generateToken(auth, "refresh", usuarioInsertado.getId_usuario());

            return new AuthResponse(true, "Usuario creado con éxito", usuarioInsertado, accessToken, refreshToken);

        } catch (DataIntegrityViolationException e) {
            System.err.println("El usuario ya existe");
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
    public Usuario getUsuario(Long id_usuario) {
        return this.usuarioRepo.findUsuarioForId(id_usuario).orElseThrow(() -> {
            System.err.println("Error en obtener el usuario con ID " + id_usuario);
            return new EntityNotFoundException("Error en obtener el usuario con ID " + id_usuario);
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
    public String getNombreUsuario(Long id_usuario) {
        return this.usuarioRepo.findNombreUsuarioForId(id_usuario).orElseThrow(() -> {
            System.err.println("Error en obtener el nombre del usuario con ID " + id_usuario);
            return new EntityNotFoundException("Error en obtener el nombre del usuario con ID " + id_usuario);
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
    public List<String> getFotosUsuario(Long id_usuario) {
        return this.usuarioRepo.findUsuarioForId(id_usuario)
                .map(Usuario::getFoto_usuario)
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
    public RoleEnum getRollUsuario(Long id_usuario) {
        return this.usuarioRepo.findRollUsuarioForId(id_usuario).orElseThrow(() -> {
            System.err.println("Error en obtener el rol del usuario con ID " + id_usuario);
            return new EntityNotFoundException("Error en obtener el rol del usuario con ID " + id_usuario);
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
    public Plan getPlanUsuario(Long id_usuario) {
        return this.usuarioRepo.findPlanUsuarioForId(id_usuario).orElseThrow(() -> {
            System.err.println("Error en obtener el plan del usuario con ID " + id_usuario);
            return new EntityNotFoundException("Error en obtener el plan del usuario con ID " + id_usuario);
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
    public List<Curso> getCursosUsuario(Long id_usuario) {
        return this.usuarioRepo.findUsuarioForId(id_usuario)
                .map(Usuario::getCursos_usuario)
                .orElse(null);
    }

    /**
     * Función para actualizar un usuario
     * 
     * @param usuario Objeto Usuario con los datos del usuario
     * @return Usuario con los datos actualizados
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     * @throws IllegalStateException    si el usuario no está autenticado
     * @throws AccessDeniedException    si el usuario no tiene permiso para acceder
     *                                  a este recurso
     *
     */
    @Override
    public Usuario updateUsuario(Usuario usuario) {
        Usuario usuario_old = this.getUsuario(usuario.getId_usuario());

        for (String foto : usuario_old.getFoto_usuario()) {
            if (!usuario.getFoto_usuario().contains(foto)) {
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
                        System.out.println("La foto no existe: " + photoPath.toString());
                    }
                } catch (IOException e) {
                    System.err.println("Error al eliminar la foto: " + photoPath.toString() + ": " + e.getMessage());
                    throw new RuntimeException(
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
        return this.usuarioRepo.changePlanUsuarioForId(usuario.getId_usuario(), usuario.getPlan_usuario())
                .orElseThrow(() -> {
                    System.err.println("Error en cambiar el plan del usuario");
                    return new EntityNotFoundException("Error en cambiar el plan del usuario");
                });
    }

    /**
     * Función para cambiar los cursos del usuario
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
    public Integer changeCursosUsuario(CursosUsuario cursosUsuario) {
        Optional<Usuario> old_usuario = this.usuarioRepo.findUsuarioForId(cursosUsuario.getId_usuario());
        if (old_usuario.isEmpty()) {
            throw new IllegalArgumentException("El usuario no existe");
        }
        List<Curso> cursos = this.cursoRepo.findAllById(cursosUsuario.getIds_cursos());
        old_usuario.get().setCursos_usuario(cursos);

        try {
            // Añadir el usuario al microservicio de stream
            WebClient webClientStream = createSecureWebClient(backStreamURL);
            webClientStream.put().uri("/nuevoCursoUsuario")
                    .body(Mono.just(old_usuario), Usuario.class)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class).flatMap(errorBody -> {
                                System.err.println("Error HTTP del microservicio de stream: " + errorBody);
                                return Mono.error(new RuntimeException("Error del microservicio: " + errorBody));
                            }))
                    .bodyToMono(String.class)
                    .onErrorResume(e -> {
                        System.err.println("Error al conectar con el microservicio de stream: " + e.getMessage());
                        return Mono.empty(); // Continuar sin interrumpir la aplicación
                    }).subscribe(res -> {
                        // Maneja el resultado cuando esté disponible
                        if (res == null || !res.equals("Usuario creado con exito!!!")) {
                        } else {
                            System.err.println("Error en crear el usuario en el stream:");
                            System.err.println(res);
                        }
                    });
        } catch (Exception e) {
            System.err.println("Error en crear el usuario en el stream: " + e.getMessage());
        }

        // Añadir el usuario al microservicio de chat
        // TODO: Implementar la lógica para añadir el usuario al microservicio de chat

        return this.usuarioRepo.changeUsuarioForId(cursosUsuario.getId_usuario(), old_usuario.get()).orElseThrow(() -> {
            System.err.println("Error en cambiar los cursos del usuario");
            return new RuntimeException("Error en cambiar los cursos del usuario");
        });
    }

    /**
     * Función para eliminar un usuario
     * 
     * @param id ID del usuario
     * @return String con el resultado de la operación
     * @throws EntityNotFoundException  si el usuario no existe
     * @throws RuntimeException         si ocurre un error en el servidor
     * @throws IllegalArgumentException si el ID no es válido
     * 
     */
    @Override
    public String deleteUsuario(Long id) {
        this.usuarioRepo.findUsuarioForId(id).orElseThrow(() -> {
            System.err.println("Error en obtener el usuario con ID " + id);
            return new EntityNotFoundException("Error en obtener el usuario con ID " + id);
        });
        try {
            this.loginRepo.deleteById(id);
            this.usuarioRepo.deleteById(id);
            return "Usuario eliminado con éxito!!!";
        } catch (IllegalArgumentException e) {
            System.out.println("Error en eliminar el usuario con ID " + id);
            throw new IllegalArgumentException("Error en eliminar el usuario con ID " + id);
        } catch (Exception e) {
            System.out.println("Error en eliminar el usuario con ID " + id);
            throw new RuntimeException("Error en eliminar el usuario con ID " + id);
        }

        // TODO: Eliminar el usuario en ambos microservicios
    }

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
    public boolean sendConfirmationEmail(NewUsuario newUsuario) {
        Context context = new Context();
        String token = jwtUtil.generateRegistrationToken(newUsuario);
        context.setVariable("nombre", newUsuario.getNombre_usuario());
        context.setVariable("link", frontURL + "/confirm-email?token=" + token);
        context.setVariable("currentYear", Year.now().getValue());

        String htmlContent = templateEngine.process("mail-registro", context);

        // Enviar el correo como HTML
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(newUsuario.getCorreo_electronico());
            helper.setSubject("Confirmación de Correo Electrónico");
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            return true;
        } catch (MailAuthenticationException e) {
            // Error de autenticación con el servidor SMTP
            System.err.println("Error de autenticación al enviar el correo: " + e.getMessage());
            throw new RuntimeException("Error de autenticación al enviar el correo: " + e.getMessage());
        } catch (MailSendException e) {
            // Error al enviar el mensaje
            System.err.println("Error al enviar el correo: " + e.getMessage());
            throw new RuntimeException("Error al enviar el correo: " + e.getMessage());
        } catch (MailException e) {
            // Otros errores relacionados con el envío de correos
            System.err.println("Error general al enviar el correo: " + e.getMessage());
            throw new RuntimeException("Error general al enviar el correo: " + e.getMessage());
        } catch (MessagingException e) {
            // Error al construir el mensaje MIME
            System.err.println("Error al construir el mensaje de correo: " + e.getMessage());
            throw new RuntimeException("Error al construir el mensaje de correo: " + e.getMessage());
        } catch (Exception e) {
            // Cualquier otro error inesperado
            System.err.println("Error inesperado al enviar el correo: " + e.getMessage());
            throw new RuntimeException("Error inesperado al enviar el correo: " + e.getMessage());
        }
    }

    public List<Usuario> getAllUsuarios() {
        try {
            return this.usuarioRepo.findAll();
        } catch (Exception e) {
            System.err.println("Error al obtener todos los usuarios: " + e.getMessage());
            throw new RuntimeException("Error al obtener todos los usuarios: " + e.getMessage());
        }
    }

    private WebClient createSecureWebClient(String baseUrl) throws Exception {

        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        // Configurar HttpClient con el contexto SSL
        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext));

        // Obtener token
        String authToken = this.jwtUtil.generateToken(null, "server", null);

        // Conectar HttpClient con WebClient y añadir header por defecto
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + authToken)
                .build();
    }
}
