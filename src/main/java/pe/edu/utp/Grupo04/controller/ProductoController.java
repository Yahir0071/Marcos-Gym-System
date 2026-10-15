package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.ProductoDTO;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.service.ProductoService;

@Controller
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // --- CATÁLOGO PÚBLICO ---
    @GetMapping("/productos")
    public String productosPublico(Model model) {
        model.addAttribute("productos", productoService.listarCatalogoActivo());
        return "public/productos";
    }

    // --- DETALLE DE PRODUCTO PÚBLICO ---
    @GetMapping({"/detalle-producto", "/public/detalle-producto"})
    public String detalleProductoPublico(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            productoService.buscarPorId(id).ifPresent(p -> model.addAttribute("producto", p));
        }
        return "public/detalle-producto";
    }

    // --- DETALLE DE PRODUCTO CLIENTE ---
    @GetMapping("/cliente/detalle-producto")
    public String detalleProductoCliente(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            productoService.buscarPorId(id).ifPresent(p -> model.addAttribute("producto", p));
        }
        return "cliente/detalle-producto";
    }

    // --- CATÁLOGO PARA CLIENTE AUTENTICADO ---
    @GetMapping("/cliente/productos")
    public String productosCliente(Model model) {
        model.addAttribute("productos", productoService.listarCatalogoActivo());
        return "cliente/productos";
    }

    // --- INVENTARIO PARA ADMINISTRACIÓN ---
    @GetMapping("/admin/productos")
    public String productosAdmin(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return "admin/productos";
    }

    @PostMapping("/admin/productos/guardar")
    public String guardarProductoAdmin(@Valid @ModelAttribute("productoDTO") ProductoDTO productoDTO,
                                       BindingResult result,
                                       RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/productos";
        }

        Producto producto = new Producto();
        if (productoDTO.getId() != null) {
            producto.setId(productoDTO.getId());
        }
        producto.setNombre(productoDTO.getNombre());
        producto.setDescripcion(productoDTO.getDescripcion());
        producto.setPrecio(productoDTO.getPrecio());
        producto.setStock(productoDTO.getStock());
        producto.setImagenUrl(productoDTO.getImagenUrl());
        producto.setActivo(productoDTO.getActivo() != null ? productoDTO.getActivo() : true);

        productoService.guardar(producto);
        flash.addFlashAttribute("mensajeExito", "Producto actualizado en el inventario.");
        return "redirect:/admin/productos";
    }

    @PostMapping("/admin/productos/cambiar-estado")
    public String cambiarEstadoProductoAdmin(@RequestParam Long id,
                                             @RequestParam boolean activo,
                                             RedirectAttributes flash) {
        try {
            productoService.cambiarEstado(id, activo);
            flash.addFlashAttribute("mensajeExito", "Estado del producto actualizado exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", "Error al cambiar estado: " + e.getMessage());
        }
        return "redirect:/admin/productos";
    }
}
