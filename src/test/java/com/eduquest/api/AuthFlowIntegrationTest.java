package com.eduquest.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient client() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    @DisplayName("Registro, login, acceso al perfil y refresh token funcionan")
    void authFlow() {
        RestClient client = client();
        Map<String, String> registerBody = Map.of(
                "username", "testuser",
                "email", "testuser@utec.edu.pe",
                "password", "Password123");

        ResponseEntity<Map> registerResponse = client.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerBody)
                .retrieve()
                .toEntity(Map.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getHeaders().getContentType()).isNotNull();

        String accessToken = (String) registerResponse.getBody().get("token");
        String refreshToken = (String) registerResponse.getBody().get("refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        Map<String, String> loginBody = Map.of("username", "testuser", "password", "Password123");
        ResponseEntity<Map> loginResponse = client.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginBody)
                .retrieve()
                .toEntity(Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> meResponse = client.get()
                .uri("/api/v1/users/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toEntity(Map.class);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody().get("username")).isEqualTo("testuser");
        assertThat((Collection<String>) meResponse.getBody().get("roles")).contains("ROLE_USER");

        Map<String, String> refreshBody = Map.of("refreshToken", refreshToken);
        ResponseEntity<Map> refreshResponse = client.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(refreshBody)
                .retrieve()
                .toEntity(Map.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((String) refreshResponse.getBody().get("token")).isNotBlank();
    }

    @Test
    @DisplayName("Sin token, los endpoints protegidos devuelven 401")
    void protectedEndpointRequiresAuth() {
        ResponseEntity<Map> response = client().get()
                .uri("/api/v1/users/me")
                .exchange((request, httpResponse) ->
                        new ResponseEntity<>(Map.of(), httpResponse.getStatusCode()));

        HttpStatusCode statusCode = response.getStatusCode();
        assertThat(statusCode.isSameCodeAs(HttpStatus.UNAUTHORIZED)).isTrue();
    }

    @Test
    @DisplayName("Los listados devuelven una página con metadatos")
    void documentsArePaginated() {
        RestClient client = client();
        Map<String, String> registerBody = Map.of(
                "username", "pager",
                "email", "pager@utec.edu.pe",
                "password", "Password123");

        Map register = client.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerBody)
                .retrieve()
                .body(Map.class);
        String token = (String) register.get("token");

        Map<String, String> documentBody = Map.of(
                "title", "Apunte paginado",
                "fileUrl", "https://cloud.example/paginado.pdf");
        client.post()
                .uri("/api/v1/documents")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(documentBody)
                .retrieve()
                .toBodilessEntity();

        ResponseEntity<Map> pageResponse = client.get()
                .uri("/api/v1/documents?page=0&size=1")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toEntity(Map.class);

        assertThat(pageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = pageResponse.getBody();
        assertThat(body).containsKey("content");
        assertThat(((Collection<?>) body.get("content"))).hasSize(1);
        long totalElements = ((Number) body.get("totalElements")).longValue();
        int totalPages = ((Number) body.get("totalPages")).intValue();
        assertThat(totalElements).isGreaterThanOrEqualTo(1L);
        assertThat(totalPages).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Solo los miembros de un grupo pueden subir documentos al grupo")
    void groupDocumentUploadRequiresMembership() {
        RestClient client = client();

        Map member = client.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "gmember", "email", "gmember@utec.edu.pe",
                        "password", "Password123"))
                .retrieve()
                .body(Map.class);
        String memberToken = (String) member.get("token");

        Map outsider = client.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "goutsider", "email", "goutsider@utec.edu.pe",
                        "password", "Password123"))
                .retrieve()
                .body(Map.class);
        String outsiderToken = (String) outsider.get("token");

        Map createdGroup = client.post()
                .uri("/api/v1/groups")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", "Grupo de Control"))
                .retrieve()
                .body(Map.class);
        long groupId = ((Number) createdGroup.get("id")).longValue();

        Map<String, String> documentBody = Map.of(
                "title", "Documento interno",
                "fileUrl", "https://cloud.example/interno.pdf");

        HttpStatusCode outsiderStatus = client.post()
                .uri("/api/v1/groups/{id}/documents", groupId)
                .header("Authorization", "Bearer " + outsiderToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(documentBody)
                .exchange((request, httpResponse) ->
                        new ResponseEntity<>(Map.of(), httpResponse.getStatusCode()))
                .getStatusCode();
        assertThat(outsiderStatus.isSameCodeAs(HttpStatus.FORBIDDEN)).isTrue();

        ResponseEntity<Map> memberUpload = client.post()
                .uri("/api/v1/groups/{id}/documents", groupId)
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(documentBody)
                .retrieve()
                .toEntity(Map.class);
        assertThat(memberUpload.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(memberUpload.getBody().get("groupName")).isEqualTo("Grupo de Control");
    }

    @Test
    @DisplayName("ADMIN lista y cambia roles; MANAGER lista pero no cambia roles; USER no lista")
    void adminRoleManagement() {
        RestClient client = client();

        Map admin = client.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "admin", "password", "admin123"))
                .retrieve()
                .body(Map.class);
        String adminToken = (String) admin.get("token");
        assertThat(adminToken).isNotBlank();

        Map target = client.post()
                .uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "targetuser", "email", "targetuser@utec.edu.pe",
                        "password", "Password123"))
                .retrieve()
                .body(Map.class);
        long targetId = ((Number) target.get("id")).longValue();
        String targetToken = (String) target.get("token");

        HttpStatusCode userStatus = client.get()
                .uri("/api/v1/admin/users?page=0&size=10")
                .header("Authorization", "Bearer " + targetToken)
                .exchange((request, httpResponse) ->
                        new ResponseEntity<>(Map.of(), httpResponse.getStatusCode()))
                .getStatusCode();
        assertThat(userStatus.isSameCodeAs(HttpStatus.FORBIDDEN)).isTrue();

        ResponseEntity<Map> listOk = client.get()
                .uri("/api/v1/admin/users?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(Map.class);
        assertThat(listOk.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> promoted = client.patch()
                .uri("/api/v1/admin/users/{id}/role?role=ROLE_MANAGER", targetId)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(Map.class);
        assertThat(promoted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Collection<String>) promoted.getBody().get("roles")).contains("ROLE_MANAGER");

        ResponseEntity<Map> listAsManager = client.get()
                .uri("/api/v1/admin/users?page=0&size=10")
                .header("Authorization", "Bearer " + targetToken)
                .retrieve()
                .toEntity(Map.class);
        assertThat(listAsManager.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpStatusCode managerPatch = client.patch()
                .uri("/api/v1/admin/users/{id}/role?role=ROLE_ADMIN", targetId)
                .header("Authorization", "Bearer " + targetToken)
                .exchange((request, httpResponse) ->
                        new ResponseEntity<>(Map.of(), httpResponse.getStatusCode()))
                .getStatusCode();
        assertThat(managerPatch.isSameCodeAs(HttpStatus.FORBIDDEN)).isTrue();
    }
}