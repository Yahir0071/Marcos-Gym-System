/**
 * core/navigation.js - Energy Gym Layout & Navigation Engine
 * Responsabilidad: Manejo de barra de navegación, reloj en vivo, sidebars responsivos y pase QR.
 */

import { getCsrfHeaders } from "./api-client.js";

export function initNavbar() {
    const navbar = document.querySelector(".navbar-main");
    if (navbar) {
        window.addEventListener("scroll", () => {
            if (window.scrollY > 30) navbar.classList.add("scrolled");
            else navbar.classList.remove("scrolled");
        });

        const currentPath = window.location.pathname;
        document.querySelectorAll(".navbar-main .nav-link").forEach((link) => {
            const href = link.getAttribute("href");
            if (href && currentPath.includes(href) && href !== "/") {
                document.querySelectorAll(".navbar-main .nav-link").forEach((l) => l.classList.remove("active"));
                link.classList.add("active");
            }
        });
    }
}

export function initLiveClock() {
    const liveClock = document.getElementById("liveClock");
    if (liveClock) {
        const updateClock = () => {
            const now = new Date();
            liveClock.textContent = now.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
        };
        updateClock();
        setInterval(updateClock, 1000);
    }
}

export function initSidebars() {
    const bindSidebar = (openBtnId, closeBtnId, sidebarId) => {
        const openBtn = document.getElementById(openBtnId);
        const closeBtn = document.getElementById(closeBtnId);
        const sidebar = document.getElementById(sidebarId);
        if (!sidebar) return;

        const openSidebar = () => {
            sidebar.classList.add("show");
            if (openBtn) openBtn.setAttribute("aria-expanded", "true");
            if (closeBtn) {
                closeBtn.focus();
            } else {
                const firstLink = sidebar.querySelector("a, button");
                if (firstLink) firstLink.focus();
            }
        };

        const closeSidebar = (returnFocus = false) => {
            if (!sidebar.classList.contains("show")) return;
            sidebar.classList.remove("show");
            if (openBtn) {
                openBtn.setAttribute("aria-expanded", "false");
                if (returnFocus) openBtn.focus();
            }
        };

        if (openBtn) {
            openBtn.setAttribute("aria-controls", sidebarId);
            openBtn.setAttribute("aria-expanded", "false");
            openBtn.addEventListener("click", openSidebar);
        }
        if (closeBtn) {
            closeBtn.addEventListener("click", () => closeSidebar(true));
        }

        document.addEventListener("keydown", (ev) => {
            if (ev.key === "Escape" && sidebar.classList.contains("show")) {
                closeSidebar(true);
            }
        });

        document.addEventListener("click", (ev) => {
            if (!sidebar.classList.contains("show")) return;
            const target = ev.target;
            if (target instanceof Node && !sidebar.contains(target) && (!openBtn || !openBtn.contains(target))) {
                closeSidebar(false);
            }
        });
    };
    bindSidebar("openSidebarBtn", "closeSidebarBtn", "adminSidebar");
    bindSidebar("openClientSidebarBtn", "closeClientSidebarBtn", "clientSidebar");
}

export function initQrPass() {
    const carnet = document.getElementById("carnetDigital");
    if (!carnet) return;

    const imagen = document.getElementById("carnetCodigo");
    const validar = document.getElementById("carnetValidar");
    const resultado = document.getElementById("carnetResultado");
    let operacion;

    function informar(texto, tipo) {
        resultado.textContent = texto;
        resultado.className = "small mb-0" + (tipo ? " text-" + tipo : "");
    }

    async function abrir() {
        if (carnet.open) return;
        imagen.hidden = true;
        imagen.removeAttribute("src");
        informar("", "");
        carnet.showModal();
        try {
            const { qrcode } = await import("../vendor/qrcode-generator.js");
            if (!carnet.open) return;
            const referencia = Array.from(crypto.getRandomValues(new Uint8Array(8)),
                byte => byte.toString(16).padStart(2, "0")).join("");
            const codigo = qrcode(0, "M");
            codigo.addData("ENERGY GYM CARNET " + referencia.toUpperCase(), "Alphanumeric");
            codigo.make();
            imagen.src = codigo.createDataURL(6, 24);
            imagen.hidden = false;
        } catch {
            informar("No se pudo generar el código del carnet. Cierra y vuelve a abrir esta ventana.", "warning");
        }
    }

    validar.addEventListener("click", async () => {
        const solicitud = new AbortController();
        operacion = solicitud;
        validar.disabled = true;
        informar("Consultando la vigencia de tu membresía…", "muted");
        try {
            const respuesta = await fetch(carnet.dataset.validarUrl, {
                method: "POST",
                headers: getCsrfHeaders(false),
                signal: solicitud.signal
            });
            const datos = await respuesta.json().catch(() => ({}));
            if (!carnet.open || operacion !== solicitud) return;
            if (respuesta.ok && datos.success === true && datos.status === "CONCEDIDO") {
                informar(datos.mensaje || "Acceso concedido.", "success");
            } else {
                informar(datos.mensaje || "No se pudo verificar la membresía.", "warning");
            }
        } catch (error) {
            if (error.name !== "AbortError") informar("No se pudo conectar. Inténtalo de nuevo.", "warning");
        } finally {
            if (operacion === solicitud) validar.disabled = false;
        }
    });

    carnet.addEventListener("close", () => {
        operacion?.abort();
        operacion = undefined;
        validar.disabled = false;
    });
    document.querySelectorAll("#btnTopbarQr, #btnOpenQrPass, [data-open-qr-pass], .btn-open-qr-pass")
        .forEach(boton => boton.addEventListener("click", abrir));
}
