package com.gas.sistema_gas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ClienteDTO {
    
    public record Create(
        
        // El nombre es opcional, pero Si lo dan debe guardarse como "Cliente Varios"
        String nombre,

        // El DNI es opcional, pero si lo entregan debe tener 8 dígitos
        @Pattern(regexp = "\\d{8}|^$", message = "El DNI debe tener exactamente 8 dígitos")
        String dni,

        // OBLIGATORIO: sin telefono no se puede hacer un pedido
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "9\\d{8}", message = "El teléfono debe empezar con 9 y tener 9 dígitos")
        String telefono,

        // OBLIGATORIO: El repartidor debe saber a donde ir
        @NotBlank(message = "La dirección es obligatoria")
        String direccion,

        String referencia,  // Siempre es util, pero opcional

        // El correo es opcional, pero si lo entregan debe tener un formato válido
        @Email(message = "Debe proporcionar un formato de email válido")
        String correo
    ){}

    public record SimpleResponse(
        Long id,
        String nombre,
        String dni,
        String telefono,
        String direccion,
        String referencia,
        String correo,
        Integer estado
    ){}

    public record Update(
        
        // El nombre es opcional, pero Si lo dan debe guardarse como "Cliente Varios"
        String nombre,

        // El DNI es opcional, pero si lo entregan debe tener 8 dígitos
        @Pattern(regexp = "\\d{8}|^$", message = "El DNI debe tener exactamente 8 dígitos")
        String dni,

        // OBLIGATORIO: sin teléfono no se puede hacer un pedido
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "9\\d{8}", message = "El teléfono debe empezar con 9 y tener 9 dígitos")
        String telefono,

        // OBLIGATORIO: El repartidor debe saber a donde ir
        @NotBlank(message = "La dirección es obligatoria")
        String direccion,

        String referencia,  // Siempre es util, pero opcional

        // El correo es opcional, pero si lo entregan debe tener un formato válido
        @Email(message = "Debe proporcionar un formato de email válido")
        String correo
    ){}
}
