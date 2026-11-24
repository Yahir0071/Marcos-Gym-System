import { getCsrfHeaders } from "../core/api-client.js";

export function initNotificaciones() {
    const bandeja = document.getElementById("notificaciones");
    if (!bandeja) return;

    const filtros = Array.from(bandeja.querySelectorAll("[data-filtro]"));
    const marcarTodas = bandeja.querySelector("[data-leer-todas]");
    const mensaje = document.getElementById("notifMensaje");
    const vacio = document.getElementById("notifVacio");
    let filtroActivo = "ALL";
    let guardando = false;

    const avisos = () => Array.from(bandeja.querySelectorAll("[data-clave]"));

    function actualizarVista() {
        const tarjetas = avisos();
        for (const filtro of filtros) {
            const categoria = filtro.dataset.filtro;
            const activo = categoria === filtroActivo;
            filtro.classList.toggle("btn-primary", activo);
            filtro.classList.toggle("btn-outline", !activo);
            filtro.setAttribute("aria-pressed", String(activo));
            filtro.querySelector("span").textContent = tarjetas.filter(tarjeta =>
                categoria === "ALL" || tarjeta.dataset.categoria === categoria).length;
        }
        for (const tarjeta of tarjetas) {
            tarjeta.hidden = filtroActivo !== "ALL" && tarjeta.dataset.categoria !== filtroActivo;
        }
        vacio.hidden = tarjetas.some(tarjeta => !tarjeta.hidden);
        marcarTodas.disabled = !tarjetas.some(tarjeta => tarjeta.dataset.leida === "false");
    }

    async function actualizarAviso(boton, tarjeta, archivar) {
        if (guardando) return;
        guardando = true;
        boton.disabled = true;
        mensaje.textContent = "";
        try {
            const destino = new URL(archivar ? bandeja.dataset.archivar : bandeja.dataset.leer, window.location.href);
            if (tarjeta) destino.searchParams.set("clave", tarjeta.dataset.clave);
            const respuesta = await fetch(destino, { method: "POST", headers: getCsrfHeaders(false) });
            if (respuesta.status !== 204) {
                if (respuesta.status === 401) {
                    mensaje.textContent = "Tu sesión terminó. Vuelve a iniciar sesión para guardar cambios.";
                } else if (respuesta.status === 404) {
                    mensaje.textContent = "El aviso cambió. Actualiza la página para ver los avisos disponibles.";
                } else {
                    mensaje.textContent = "No se pudo guardar el cambio. Inténtalo de nuevo.";
                }
                return;
            }
            if (archivar) {
                tarjeta.remove();
                mensaje.textContent = "Aviso archivado.";
            } else {
                for (const aviso of tarjeta ? [tarjeta] : avisos()) {
                    aviso.dataset.leida = "true";
                    aviso.querySelector("[data-no-leida]")?.remove();
                    aviso.querySelector("[data-leer-aviso]")?.remove();
                }
                mensaje.textContent = tarjeta ? "Aviso marcado como leído." : "Todos los avisos están marcados como leídos.";
            }
            if (!boton.isConnected || boton === marcarTodas) {
                const siguiente = !archivar && tarjeta?.querySelector("[data-archivar-aviso]");
                (siguiente || filtros.find(filtro => filtro.dataset.filtro === filtroActivo))?.focus();
            }
        } catch {
            mensaje.textContent = "No hay conexión. Inténtalo de nuevo.";
        } finally {
            guardando = false;
            boton.disabled = false;
            actualizarVista();
        }
    }

    bandeja.addEventListener("click", evento => {
        const filtro = evento.target.closest("[data-filtro]");
        if (filtro) {
            filtroActivo = filtro.dataset.filtro;
            actualizarVista();
            return;
        }
        const boton = evento.target.closest("[data-leer-todas], [data-leer-aviso], [data-archivar-aviso]");
        if (boton) actualizarAviso(boton, boton.closest("[data-clave]"), boton.hasAttribute("data-archivar-aviso"));
    });
    actualizarVista();
}
