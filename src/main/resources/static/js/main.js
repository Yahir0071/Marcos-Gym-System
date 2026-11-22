import { showGymToast, initDataToastTriggers } from "./core/toast.js";
import { openGymModal, openSafeGymModal, closeGymModal } from "./core/modal.js";
import { initNavbar, initLiveClock, initSidebars, initQrPass } from "./core/navigation.js";

// Los scripts de las vistas usan estas utilidades para los avisos y modales.
Object.assign(window, { showGymToast, openGymModal, openSafeGymModal, closeGymModal });

const cargar = (modulo, iniciar) => {
    modulo.then(iniciar).catch(error => console.error("No se pudo iniciar un componente de la página.", error));
};

document.addEventListener("DOMContentLoaded", () => {
    initNavbar();
    initLiveClock();
    initSidebars();
    initQrPass();
    initDataToastTriggers();

    const ruta = window.location.pathname;
    if (document.getElementById("bmiCalculatorForm")) {
        cargar(import("./public/calculator.js"), m => m.initBmiCalculator());
    }
    if (document.getElementById("billingSwitcher")) {
        cargar(import("./public/planes.js"), m => m.initBillingSwitcher());
    }
    if (ruta === "/promociones") {
        cargar(import("./public/promociones.js"), m => {
            m.initPromoCountdown();
            m.initPromoCoupons();
        });
    }
    if (document.querySelector(".day-afluencia-btn")) {
        cargar(import("./public/afluencia.js"), m => m.initAfluenciaSelector());
    }
    if (["/productos", "/detalle-producto", "/public/detalle-producto",
        "/cliente/productos", "/cliente/detalle-producto"].includes(ruta)) {
        cargar(import("./public/catalogo.js"), m => {
            m.initProductCatalog();
            m.initProductDetail();
        });
    }
    if (document.getElementById("togglePasswordBtn")) {
        cargar(import("./auth/login.js"), m => m.initLoginControls());
    }
    if (document.getElementById("weeklyWorkoutGrid")) {
        cargar(import("./cliente/rutina.js"), m => m.initWorkoutTracker());
    }
    if (ruta === "/cliente/mi-membresia" || ruta === "/cliente/mis-reservas") {
        cargar(import("./cliente/congelamiento.js"), m => {
            m.initCongelamiento();
            m.initSafeReservaVoucher();
        });
    }
    if (ruta === "/cliente/notificaciones") {
        cargar(import("./cliente/notificaciones.js"), m => m.initNotificaciones());
    } else if (ruta.startsWith("/cliente/") &&
        !["/cliente/mi-membresia", "/cliente/mis-reservas", "/cliente/afluencia"].includes(ruta)) {
        cargar(import("./cliente/portal.js"), m => m.initCartAndCompras());
    }
    if (ruta === "/admin" || ruta.startsWith("/admin/")) {
        cargar(import("./admin/tables-crud.js"), m => {
            m.initTableFilters();
            m.initDashboardChart();
            m.initAdminTabs();
            m.initAdminCrudModals();
        });
    } else if (ruta === "/cliente/mis-reservas") {
        cargar(import("./admin/tables-crud.js"), m => m.initTableFilters());
    }
});
