package com.eduquest.api.controller;

import com.eduquest.api.dto.request.ChangePasswordRequestDTO;
import com.eduquest.api.dto.request.UpdateProfileRequestDTO;
import com.eduquest.api.dto.response.AuthResponseDTO;
import com.eduquest.api.dto.response.AvatarImageDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.security.UserPrincipal;
import com.eduquest.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios", description = "Perfil del usuario autenticado y foto de perfil.")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil del usuario autenticado", description = "Devuelve id, username, email, roles y la URL de su foto.")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getUsername()));
    }

    @PatchMapping("/me")
    @Operation(summary = "Actualizar perfil", description = "Cambia el nombre de usuario y, opcionalmente, el perfil de asesor (advisor). Devuelve tokens nuevos porque el nombre forma parte del JWT. 409 si el nombre ya existe.")
    public ResponseEntity<AuthResponseDTO> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateProfile(principal.getUsername(), request));
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir foto de perfil", description = "Imagen JPG, PNG o WEBP de hasta 2 MB en el campo 'file'.")
    public ResponseEntity<UserResponseDTO> updateAvatar(@RequestParam("file") MultipartFile file,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateAvatar(principal.getUsername(), file));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Cambiar mi contraseña", description = "Pide la contraseña actual. 400 si no coincide o si la nueva es igual.")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        userService.changePassword(principal.getUsername(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/avatar")
    @Operation(summary = "Quitar foto de perfil")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteAvatar(principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/avatar")
    @Operation(summary = "Obtener foto de perfil", description = "Público. 404 si el usuario no tiene foto.")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        AvatarImageDTO image = userService.getAvatar(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(image.data());
    }
}
