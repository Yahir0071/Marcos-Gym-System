package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;
import pe.edu.utp.Grupo04.dto.ProductoDTO;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.model.enums.CategoriaProducto;
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
    public ModelAndView detalleProductoPublico(@RequestParam(required = false) Long id) {
        return detalleProducto(id, "public/detalle-producto", "/productos");
    }

    // --- DETALLE DE PRODUCTO CLIENTE ---
    @GetMapping("/cliente/detalle-producto")
    public ModelAndView detalleProductoCliente(@RequestParam(required = false) Long id) {
        return detalleProducto(id, "cliente/detalle-producto", "/cliente/productos");
    }

    private ModelAndView detalleProducto(Long id, String vista, String catalogo) {
        if (id != null) {
            var producto = productoService.buscarPorId(id).filter(p -> Boolean.TRUE.equals(p.getActivo()));
            if (producto.isPresent()) {
                return new ModelAndView(vista, "producto", producto.get());
            }
        }
        ModelAndView respuesta = new ModelAndView("public/producto-no-disponible", "catalogo", catalogo);
        respuesta.setStatus(HttpStatus.NOT_FOUND);
        return respuesta;
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
        model.addAttribute("categoriasProducto", CategoriaProducto.values());
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
        producto.setNombre(productoDTO.getNombre().trim());
        producto.setDescripcion(productoDTO.getDescripcion() != null ? productoDTO.getDescripcion().trim() : null);
        producto.setPrecio(productoDTO.getPrecio());
        producto.setStock(productoDTO.getStock());
        producto.setImagenUrl(productoDTO.getImagenUrl() != null ? productoDTO.getImagenUrl().trim() : null);
        producto.setActivo(productoDTO.getActivo() != null ? productoDTO.getActivo() : true);
        producto.setCategoria(productoDTO.getCategoria());

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
