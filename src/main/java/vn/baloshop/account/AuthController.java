package vn.baloshop.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final CustomerAccountService customers;
    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository securityContextRepository;

    public AuthController(CustomerAccountService customers, AuthenticationManager authenticationManager,
                          HttpSessionSecurityContextRepository securityContextRepository) {
        this.customers = customers;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @GetMapping("/me")
    public AuthResponse current(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return AuthResponse.anonymous();
        }
        return AuthResponse.authenticated(customers.findByEmail(authentication.getName()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest,
                                      HttpServletResponse response) {
        CustomerAccount customer = customers.register(request);
        return authenticate(customer.getEmail(), request.password(), servletRequest, response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
                                   HttpServletResponse response) {
        return authenticate(CustomerAccountService.normalizeEmail(request.email()), request.password(),
                servletRequest, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> authenticate(String email, String password, HttpServletRequest request,
                                           HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            request.getSession(true);
            request.changeSessionId();
            securityContextRepository.saveContext(context, request, response);
            return ResponseEntity.ok(AuthResponse.authenticated(customers.findByEmail(email)));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email hoặc mật khẩu chưa chính xác."));
        }
    }
}
