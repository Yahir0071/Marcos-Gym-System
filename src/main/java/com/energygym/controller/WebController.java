package com.energygym.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    // --- ZONA PÚBLICA ---
    @GetMapping({"/", "/inicio"})
    public String inicio() {
        return "public/inicio";
    }

    @GetMapping("/planes")
    public String planes() {
        return "public/planes";
    }

    @GetMapping("/promociones")
    public String promociones() {
        return "public/promociones";
    }

    @GetMapping("/productos")
    public String productos() {
        return "public/productos";
    }

    @GetMapping("/afluencia")
    public String afluencia() {
        return "public/afluencia";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/inicio";
    }

    // --- ÁREA CLIENTE ---
    @GetMapping("/cliente/mi-membresia")
    public String miMembresia() {
        return "cliente/mi-membresia";
    }

    @GetMapping("/cliente/mis-reservas")
    public String misReservas() {
        return "cliente/mis-reservas";
    }

    @GetMapping("/cliente/productos")
    public String clienteProductos() {
        return "cliente/productos";
    }

    @GetMapping("/cliente/afluencia")
    public String clienteAfluencia() {
        return "cliente/afluencia";
    }

    @GetMapping("/cliente/notificaciones")
    public String notificaciones() {
        return "cliente/notificaciones";
    }

    @GetMapping("/detalle-producto")
    public String detalleProducto() {
        return "cliente/detalle-producto";
    }
}
