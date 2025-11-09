package com.sovereingschool.back_base.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sovereingschool.back_base.DTOs.AuthResponse;
import com.sovereingschool.back_base.DTOs.ChangePassword;
import com.sovereingschool.back_base.Interfaces.ILoginService;
import com.sovereingschool.back_common.Models.Login;
import com.sovereingschool.back_common.Models.Usuario;
import com.sovereingschool.back_common.Repositories.LoginRepository;
import com.sovereingschool.back_common.Repositories.UsuarioRepository;
import com.sovereingschool.back_common.Utils.JwtUtil;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class LoginService implements UserDetailsService, ILoginService {
    private LoginRepository loginRepository;
    private UsuarioRepository usuarioRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;

    private Logger logger = LoggerFactory.getLogger(LoginService.class);

    /**
     * Constructor de LoginService
     *
     * @param loginRepository   Repositorio de logins
     * @param usuarioRepository Repositorio de usuarios
     * @param passwordEncoder   Encriptador de contraseñas
     * @param jwtUtil           Utilidad de JWT
     */
    public LoginService(LoginRepository loginRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.loginRepository = loginRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Función para comprobar si el correo existe
     * 
     * @param correo Correo electrónico del usuario
     * @return Long con el ID del usuario
     */
    @Override
    public Long compruebaCorreo(String correo) {
        return this.loginRepository.compruebaCorreo(correo).orElse(0L);
    }

    /**
     * Función para crear un nuevo login
     * 
     * @param login Objeto Login con los datos del usuario
     * @return String con el mensaje de creación de login
     */
    @Override
    public String createNuevoLogin(Login login) {
        this.loginRepository.save(login);
        return "Nuevo Usuario creado con éxito!!!";
    }

    /**
     * Función para obtener el correo electrónico del usuario
     * 
     * @param id_usuario ID del usuario
     * @return String con el correo electrónico del usuario
     * @throws EntityNotFoundException si el usuario no existe
     */
    @Override
    public String getCorreoLogin(Long idUsuario) {
        return this.loginRepository.findCorreoLoginForId(idUsuario).orElseThrow(
                () -> {
                    logger.error("getCorreoLogin: Error en obtener el correo del usuario con ID {}", idUsuario);
                    return new EntityNotFoundException(
                            "getCorreoLogin: Error en obtener el correo del usuario con ID " + idUsuario);
                });
    }

    /**
     * Función para obtener la contraseña del usuario
     * 
     * @param id_usuario ID del usuario
     * @return String con la contraseña del usuario
     * @throws EntityNotFoundException si el usuario no existe
     */
    @Override
    public String getPasswordLogin(Long idUsuario) {
        return this.loginRepository.findPasswordLoginForId(idUsuario).orElseThrow(
                () -> {
                    logger.error("Error en obtener la contraseña del usuario con ID {}", idUsuario);
                    return new EntityNotFoundException(
                            "Error en obtener la contraseña del usuario con ID " + idUsuario);
                });
    }

    /**
     * Función para cambiar el correo electrónico del usuario
     * 
     * @param login Objeto Login con los datos del usuario
     * @return String con el mensaje de cambio de correo electrónico
     * @throws EntityNotFoundException si el usuario no existe
     */
    @Override
    public String changeCorreoLogin(Login login) {
        this.loginRepository.changeCorreoLoginForId(login.getIdUsuario(), login.getCorreoElectronico());
        return "Correo cambiado con éxito!!!";
    }

    /**
     * Función para cambiar la contraseña del login
     * 
     * @param changepassword Objeto ChangePassword con los datos del usuario
     * @return Integer con el resultado de la operación
     */
    @Override
    public Integer changePasswordLogin(ChangePassword changepassword) {
        if (changepassword.getNewPassword().isEmpty() || changepassword.getOldPassword().isEmpty())
            return null;

        // Desempaquetar Optional<String>
        String oldPasswordDB = this.loginRepository
                .findPasswordLoginForId(changepassword.getIdUsuario())
                .orElse(null);

        if (Objects.equals(oldPasswordDB, changepassword.getOldPassword())) {
            this.loginRepository.changePasswordLoginForId(changepassword.getIdUsuario(),
                    changepassword.getNewPassword());
            return 1;
        }

        return 0;
    }

    /**
     * Función para eliminar un login
     * 
     * @param idUsuario ID del usuario
     * @return String con el mensaje de eliminación de login
     */
    @Override
    public String deleteLogin(Long idUsuario) {
        this.loginRepository.deleteById(idUsuario);
        return "Login eliminado con éxito!!!";
    }

    /**
     * Función para obtener los datos del usuario a partir del correo electrónico
     * 
     * @param correo Correo electrónico del usuario
     * @return Objeto UserDetails con los datos del usuario
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        // Si es el usuario visitante, no buscar en BD
        if (correo.equals("Visitante")) {
            return User.withUsername("Visitante")
                    .password("visitante")
                    .roles("GUEST")
                    .accountExpired(false)
                    .credentialsExpired(false)
                    .accountLocked(false)
                    .build();
        }

        Optional<Login> login = this.loginRepository.getLoginForCorreo(correo);
        if (login.isEmpty()) {
            logger.error("Correo electronico {} no encontrado", correo);
            throw new UsernameNotFoundException("Correo electronico " + correo + " no encontrado");
        }

        Optional<Usuario> usuario = this.usuarioRepository.findById(login.get().getIdUsuario());
        if (usuario.isEmpty()) {
            logger.error("Usuario no encontrado en loadUserByUsername");
            throw new UsernameNotFoundException("Usuario no encontrado");
        }

        List<SimpleGrantedAuthority> roles = new ArrayList<>();
        roles.add(new SimpleGrantedAuthority("ROLE_" + usuario.get().getRollUsuario().name()));

        return new User(usuario.get().getNombreUsuario(),
                login.get().getPassword(),
                usuario.get().getIsEnabled(),
                usuario.get().getAccountNoExpired(),
                usuario.get().getCredentialsNoExpired(),
                usuario.get().getAccountNoLocked(),
                roles);
    }

    /**
     * Función para validar el login del usuario
     * 
     * @param id       ID del usuario
     * @param password Contraseña del usuario
     * @return Objeto AuthResponse con los datos del usuario
     * @throws BadCredentialsException si el usuario o contraseña son incorrectos
     * @throws EntityNotFoundException si el usuario no existe
     */
    @Override
    @Transactional
    public AuthResponse loginUser(Long id, String password) {
        String correo = this.loginRepository.findCorreoLoginForId(id)
                .orElseThrow(() -> {
                    logger.error("loginUser: Error en obtener el correo del usuario con ID {}", id);
                    return new EntityNotFoundException(
                            "loginUser: Error en obtener el correo del usuario con ID " + id);
                });
        UserDetails userDetails = this.loadUserByUsername(correo);
        if (userDetails == null) {
            throw new BadCredentialsException("Usuario o password incorrecto");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())
                && !password.equals(userDetails.getPassword())) {
            throw new BadCredentialsException("Password incorrecta");
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(correo, userDetails.getPassword(),
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Usuario> usuarioOpt = this.usuarioRepository.findById(
                this.loginRepository.getLoginForCorreo(auth.getName())
                        .orElseThrow(() -> {
                            logger.error("Error en obtener el login con el correo {}", auth.getName());
                            return new EntityNotFoundException(
                                    "Error en obtener el login con el correo " + auth.getName());
                        })
                        .getIdUsuario());
        if (usuarioOpt.isEmpty()) {
            logger.error("Usuario no encontrado en loginUser");
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        Usuario usuario = usuarioOpt.get();
        Hibernate.initialize(usuario.getCursosUsuario());

        String accessToken = jwtUtil.generateToken(auth, "access", usuario.getIdUsuario());
        String refreshToken = jwtUtil.generateToken(auth, "refresh", usuario.getIdUsuario());

        return new AuthResponse(true, "Login exitoso", usuario, accessToken, refreshToken);
    }

    /**
     * Función para obtener un nuevo token de acceso y refresco
     * 
     * @param id ID del usuario
     * @return Objeto AuthResponse con los datos del usuario
     * @throws BadCredentialsException si el usuario o contraseña son incorrectos
     * @throws EntityNotFoundException si el usuario no existe
     */
    @Override
    @Transactional
    public AuthResponse refreshAccessToken(Long id) {
        String correo = this.loginRepository.findCorreoLoginForId(id)
                .orElseThrow(() -> {
                    logger.error("refreshAccessToken: Error en obtener el correo del usuario con ID {}", id);
                    return new EntityNotFoundException(
                            "refreshAccessToken: Error en obtener el correo del usuario con ID " + id);
                });
        UserDetails userDetails = this.loadUserByUsername(correo);
        if (userDetails == null) {
            logger.error("Usuario no encontrado en refreshAccessToken");
            throw new BadCredentialsException("Usuario no encontrado");
        }

        Authentication auth = new UsernamePasswordAuthenticationToken(correo, userDetails.getPassword(),
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);
        String accessToken = jwtUtil.generateToken(auth, "access", id);
        String refreshToken = jwtUtil.generateToken(auth, "refresh", id);
        return new AuthResponse(true, "Refresh exitoso", null, accessToken, refreshToken);
    }

    /**
     * Función para hacer login con un token
     * 
     * @param token Token JWT
     * @return Objeto Usuario con los datos del usuario
     * @throws BadCredentialsException si el token no es válido
     * @throws EntityNotFoundException si el usuario no existe
     */
    @Override
    @Transactional
    public Usuario loginWithToken(String token) {
        try {
            Long idUsuario = jwtUtil.getIdUsuario(token);
            Optional<Usuario> opUsuario = this.usuarioRepository.findById(idUsuario);
            if (opUsuario.isEmpty()) {
                logger.error("Usuario no encontrado en loginWithToken: id_usuario: {}", idUsuario);
                throw new BadCredentialsException("Usuario no encontrado");
            }
            return opUsuario.get();
        } catch (JWTVerificationException | InsufficientAuthenticationException | BadCredentialsException e) {
            logger.error("Error en hacer login con token: {}", e.getMessage());
            throw new JWTVerificationException("Error en hacer login con token: " + e.getMessage());
        }
    }
}
