/**
 * main.js - Energy Gym Core Orchestrator (ES6 Modular Architecture)
 * Responsabilidad: Orquestación limpia y arranque condicional de submódulos según la vista activa.
 */

// Core Modules
import { showGymToast, initDataToastTriggers } from "./core/toast.js";
import { openGymModal, closeGymModal } from "./core/modal.js";
import { initNavbar, initLiveClock, initSidebars, initQrPass } from "./core/navigation.js";

// Public Modules
import { initBmiCalculator } from "./public/calculator.js";
import { initBillingSwitcher, initPreEnrollment } from "./public/planes.js";
import { initPromoCountdown, initPromoCoupons } from "./public/promociones.js";
import { initAfluenciaSelector } from "./public/afluencia.js";
import { initProductCatalog, initProductDetail } from "./public/catalogo.js";

// Auth Module
import { initLoginControls } from "./auth/login.js";

// Client Portal Modules
import { initWorkoutTracker, initPlanActions, initReservasClient, initNotifications } from "./cliente/portal.js";

// Admin Module
import { initTableFilters, initDashboardChart, initAdminTabs, initAdminCrudModals } from "./admin/tables-crud.js";

// Exponer utilidades globales en window para compatibilidad con triggers dinámicos
window.showGymToast = showGymToast;
window.openGymModal = openGymModal;
window.closeGymModal = closeGymModal;

document.addEventListener("DOMContentLoaded", () => {
    console.log("⚡ Energy Gym ES6 Modular System Bootstrapped");

    // 1. Inicialización de componentes base (presentes en todas las vistas)
    initNavbar();
    initLiveClock();
    initSidebars();
    initQrPass();
    initDataToastTriggers();

    // 2. Módulos Públicos (arranque bajo demanda si los elementos existen en el DOM)
    initBmiCalculator();
    initBillingSwitcher();
    initPreEnrollment();
    initPromoCountdown();
    initPromoCoupons();
    initAfluenciaSelector();
    initProductCatalog();
    initProductDetail();

    // 3. Módulo de Autenticación
    initLoginControls();

    // 4. Módulos del Portal del Cliente
    initWorkoutTracker();
    initPlanActions();
    initReservasClient();
    initNotifications();

    // 5. Módulo del Panel de Administración
    initTableFilters();
    initDashboardChart();
    initAdminTabs();
    initAdminCrudModals();
});
