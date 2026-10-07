package com.atlas.supplychain.auth.shared.infrastructure.dto.maintenance.authorization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String idUser;

    private String idCompny;

    // Generado por el backend (nombre.apellido + sufijo si colisiona) -- nunca lo manda el
    // cliente al crear, se ignora si lo manda; presente acá solo para lectura (obtenerUsuarioPorId,
    // listarUsuarios...).
    private String username;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 100)
    private String name;

    @NotBlank(groups = {Crear.class, Actualizar.class})
    @Size(max = 100)
    private String lastName;

    @Email(groups = {Crear.class, Actualizar.class})
    @NotBlank(groups = {Crear.class})
    @Size(max = 150)
    private String email;

    @NotBlank(groups = {Crear.class})
    @Size(min = 6, groups = {Crear.class})
    private String password;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private boolean state;

    @NotNull(groups = {Crear.class, Actualizar.class})
    private Long idRole;

    public interface Crear {}
    public interface Actualizar {}
}