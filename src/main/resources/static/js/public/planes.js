/**
 * public/planes.js - Energy Gym Membership Plans Engine
 * Responsabilidad: Selector dinámico de periodos de facturación y actualización sincronizada de contratación.
 */

import { showGymToast } from "../core/toast.js";

export function initBillingSwitcher() {
    const billingBtns = document.querySelectorAll(".billing-period-btn");
    if (billingBtns.length === 0) return;

    function updateEnrollLinks(mult) {
        const mode = mult === 3 ? "TRIMESTRAL" : (mult === 12 ? "ANUAL" : "MENSUAL");
        document.querySelectorAll(".btn-enroll-plan").forEach((link) => {
            const planId = link.getAttribute("data-plan-id");
            const isAuth = link.getAttribute("data-auth") === "true";
            const targetBase = isAuth ? "/cliente/pago-en-linea" : "/registrarse";
            const params = new URLSearchParams({ planId, periodo: mode });
            if (!isAuth) params.set("meses", String(mult));
            link.href = `${targetBase}?${params}`;
        });
    }

    const formatoPrecio = new Intl.NumberFormat("es-PE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    billingBtns.forEach((btn) => {
        btn.addEventListener("click", function () {
            billingBtns.forEach((b) => {
                b.classList.remove("btn-primary", "active");
                b.classList.add("btn-outline");
                b.setAttribute("aria-pressed", "false");
            });
            this.classList.remove("btn-outline");
            this.classList.add("btn-primary", "active");
            this.setAttribute("aria-pressed", "true");

            const mult = parseFloat(this.getAttribute("data-multiplier")) || 1;
            const disc = parseFloat(this.getAttribute("data-discount")) || 1;
            const label = this.getAttribute("data-label") || "AL MES";

            document.querySelectorAll(".plan-price").forEach((priceEl) => {
                const base = Number(priceEl.dataset.basePrice);
                if (Number.isFinite(base)) priceEl.textContent = `S/ ${formatoPrecio.format(base * mult * disc)}`;
            });
            document.querySelectorAll(".plan-period-label").forEach((lbl) => {
                lbl.textContent = label;
            });

            updateEnrollLinks(mult);
            showGymToast(`Tarifas actualizadas a modalidad: ${label}`, "fa-tags");
        });
    });

    // Inicializar URLs al cargar
    updateEnrollLinks(1);
}
