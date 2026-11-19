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
