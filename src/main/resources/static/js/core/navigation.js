/**
 * core/navigation.js - Energy Gym Layout & Navigation Engine
 * Responsabilidad: Manejo de barra de navegación, reloj en vivo, sidebars responsivos y pase QR.
 */

import { openGymModal } from "./modal.js";
import { showGymToast } from "./toast.js";

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
        if (openBtn && sidebar) openBtn.addEventListener("click", () => sidebar.classList.add("show"));
        if (closeBtn && sidebar) closeBtn.addEventListener("click", () => sidebar.classList.remove("show"));
    };
    bindSidebar("openSidebarBtn", "closeSidebarBtn", "adminSidebar");
    bindSidebar("openClientSidebarBtn", "closeClientSidebarBtn", "clientSidebar");
}

export function initQrPass() {
    const triggerQrModal = () => {
        const token = "EG-" + Math.floor(100000 + Math.random() * 900000);
        openGymModal(
            `<i class="fa-solid fa-qrcode me-2"></i> PASE DIGITAL DE ACCESO`,
            `<div class="text-center p-2">
                <div class="badge bg-primary text-dark font-anton mb-3 px-3 py-1">SOCIO ACTIVO · PLAN VIGENTE</div>
                <h4 class="font-anton text-light mb-3">ACCESO BIOMÉTRICO</h4>
                <div class="bg-white p-3 rounded d-inline-block mb-3" style="width: 190px; height: 190px;">
                    <svg viewBox="0 0 100 100" width="100%" height="100%" fill="#000">
                        <rect x="5" y="5" width="28" height="28" fill="none" stroke="#000" stroke-width="6"/>
                        <rect x="13" y="13" width="12" height="12"/>
                        <rect x="67" y="5" width="28" height="28" fill="none" stroke="#000" stroke-width="6"/>
                        <rect x="75" y="13" width="12" height="12"/>
                        <rect x="5" y="67" width="28" height="28" fill="none" stroke="#000" stroke-width="6"/>
                        <rect x="13" y="75" width="12" height="12"/>
                        <rect x="42" y="10" width="8" height="8"/><rect x="52" y="18" width="8" height="16"/>
                        <rect x="40" y="40" width="20" height="20"/><rect x="10" y="42" width="18" height="8"/>
                        <rect x="68" y="44" width="12" height="18"/><rect x="44" y="68" width="16" height="10"/>
                        <rect x="66" y="70" width="24" height="10"/><rect x="74" y="84" width="18" height="10"/>
                    </svg>
                </div>
                <div class="text-xs text-muted mb-3">TOKEN DINÁMICO: <strong class="text-primary">${token}</strong></div>
                <p class="text-muted text-xs mb-4">Acerca este código al lector óptico del torniquete en el local.</p>
                <button type="button" class="btn btn-primary w-100" data-bs-dismiss="modal" id="btnSimulateTorniquete">
                    SIMULAR VALIDACIÓN EN TORNIQUETE
                </button>
            </div>`
        );

        setTimeout(() => {
            const simBtn = document.getElementById("btnSimulateTorniquete");
            if (simBtn) {
                simBtn.addEventListener("click", () => {
                    showGymToast("¡Ingreso registrado en torniquete principal!", "fa-door-open");
                });
            }
        }, 50);
    };

    const btnOpenQrPass = document.getElementById("btnOpenQrPass");
    const btnTopbarQr = document.getElementById("btnTopbarQr");
    if (btnOpenQrPass) btnOpenQrPass.addEventListener("click", triggerQrModal);
    if (btnTopbarQr) btnTopbarQr.addEventListener("click", triggerQrModal);
}
