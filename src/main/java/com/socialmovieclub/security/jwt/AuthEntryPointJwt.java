package com.socialmovieclub.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.socialmovieclub.core.result.RestResponse;
import com.socialmovieclub.core.utils.MessageHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Slf4j
@RequiredArgsConstructor // MessageHelper'ı enjekte etmek için ekledik
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private final MessageHelper messageHelper; // Merkezi mesaj yönetimi

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {

        // JwtUtils tarafından set edilen özel hatayı al, yoksa varsayılanı kullan
        String errorKey = (String) request.getAttribute("jwt_error");
        if (errorKey == null) {
            errorKey = "auth.unauthorized";
        }

        String errorMessage = messageHelper.getMessage(errorKey);
        RestResponse<Object> restResponse = RestResponse.error(errorMessage, HttpStatus.UNAUTHORIZED);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.writeValue(response.getOutputStream(), restResponse);
    }
}