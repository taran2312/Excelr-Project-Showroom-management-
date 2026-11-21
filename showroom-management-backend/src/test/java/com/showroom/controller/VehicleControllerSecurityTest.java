package com.showroom.controller;

import com.showroom.config.security.CustomUserDetailsService;
import com.showroom.config.security.JwtAuthEntryPoint;
import com.showroom.config.security.JwtAuthenticationFilter;
import com.showroom.config.security.JwtUtil;
import com.showroom.config.security.SecurityConfig;
import com.showroom.dto.VehicleDto;
import com.showroom.entity.Role;
import com.showroom.entity.User;
import com.showroom.entity.VehicleStatus;
import com.showroom.repository.UserRepository;
import com.showroom.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web layer + real security chain: JWT filter, 401 entry point, role checks, validation errors. */
@WebMvcTest(VehicleController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthEntryPoint.class, JwtUtil.class,
        CustomUserDetailsService.class})
@TestPropertySource(properties = {
        "app.jwt.secret=wtBKz1tndgvK2TsogXRiyUs2sfVxOjaOETxpI0l5tQoURqV1wt6knDIyTTDH/cCQkZJWCxhESv6SPkFWxTvTNQ==",
        "app.jwt.expiration-ms=60000",
        "app.cors.allowed-origins=http://localhost:5173"
})
class VehicleControllerSecurityTest {

    private static final String VALID_BODY = """
            {"vin":"VIN0000000000001","make":"Honda","model":"City","year":2026,"price":1500000,"showroomId":1}
            """;

    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwtUtil;
    @MockitoBean VehicleService vehicleService;
    @MockitoBean UserRepository userRepository;

    @Test
    void requestWithoutTokenGetsJson401() throws Exception {
        mvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void requestWithInvalidTokenGets401() throws Exception {
        mvc.perform(get("/api/vehicles").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    /** Issues a real JWT for an active user with the given role, as the login endpoint would. */
    private String bearer(Role role) {
        String email = role.name().toLowerCase() + "@showroom.com";
        User user = User.builder().id(1L).email(email).password("hash").fullName(role.name())
                .role(role).active(true).build();
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
        return "Bearer " + jwtUtil.generateToken(email, Map.of("role", role.name()));
    }

    @Test
    void validJwtIsAccepted() throws Exception {
        when(vehicleService.search(null, null, null)).thenReturn(List.of());

        mvc.perform(get("/api/vehicles").header("Authorization", bearer(Role.SALES)))
                .andExpect(status().isOk());
    }

    @Test
    void jwtForDeactivatedUserIsRejected() throws Exception {
        User user = User.builder().id(1L).email("gone@showroom.com").password("hash").fullName("Gone")
                .role(Role.ADMIN).active(false).build();
        when(userRepository.findByEmailIgnoreCase("gone@showroom.com")).thenReturn(Optional.of(user));
        String token = jwtUtil.generateToken("gone@showroom.com", Map.of());

        mvc.perform(get("/api/vehicles").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void salesRoleCannotCreateVehicles() throws Exception {
        mvc.perform(post("/api/vehicles").header("Authorization", bearer(Role.SALES))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(vehicleService);
    }

    @Test
    void managerCanCreateVehicles() throws Exception {
        when(vehicleService.create(any())).thenReturn(VehicleDto.builder().id(7L).vin("VIN0000000000001")
                .make("Honda").model("City").year(2026).price(new BigDecimal("1500000")).showroomId(1L)
                .status(VehicleStatus.AVAILABLE).build());

        mvc.perform(post("/api/vehicles").header("Authorization", bearer(Role.MANAGER))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void invalidBodyReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/vehicles").header("Authorization", bearer(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"year\":1900}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.vin").exists())
                .andExpect(jsonPath("$.fieldErrors.year").exists())
                .andExpect(jsonPath("$.fieldErrors.showroomId").exists());
    }
}
