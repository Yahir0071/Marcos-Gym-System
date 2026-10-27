/**
 * public/planes.js - Energy Gym Membership Plans Engine
 * Responsabilidad: Selector dinámico de periodos de facturación y actualización sincronizada de contratación.
 */

import { showGymToast } from "../core/toast.js";

export function initBillingSwitcher() {
    const billingBtns = document.querySelectorAll(".billing-period-btn");
    if (billingBtns.length === 0) return;

    function updateEnrollLinks(mult, label, disc) {
        const mode = mult === 3 ? "TRIMESTRAL" : (mult === 12 ? "ANUAL" : "MENSUAL");
        document.querySelectorAll(".btn-enroll-plan").forEach((link) => {
            const planId = link.getAttribute("data-plan-id");
            const card = link.closest(".card");
            const priceEl = card ? card.querySelector(".plan-price") : null;
            const currentPrice = priceEl ? priceEl.textContent.replace(/[^0-9]/g, "") : "";
            
            const isAuth = link.getAttribute("data-auth") === "true";
            const targetBase = isAuth ? "/cliente/pago-en-linea" : "/registrarse";
            link.setAttribute("href", `${targetBase}?planId=${planId}&periodo=${mode}&meses=${mult}&precio=${currentPrice}`);
        });
    }

    billingBtns.forEach((btn) => {
        btn.addEventListener("click", function () {
            billingBtns.forEach((b) => {
                b.classList.remove("btn-primary", "active");
                b.classList.add("btn-outline");
            });
            this.classList.remove("btn-outline");
            this.classList.add("btn-primary", "active");

            const mult = parseFloat(this.getAttribute("data-multiplier")) || 1;
            const disc = parseFloat(this.getAttribute("data-discount")) || 1;
            const label = this.getAttribute("data-label") || "AL MES";

            document.querySelectorAll(".plan-price").forEach((priceEl) => {
                const base = parseFloat(priceEl.getAttribute("data-base-price")) || 79;
                const finalPrice = Math.round(base * mult * disc);
                priceEl.textContent = `S/ ${finalPrice}`;
            });
            document.querySelectorAll(".plan-period-label").forEach((lbl) => {
                lbl.textContent = label;
            });

            updateEnrollLinks(mult, label, disc);
            showGymToast(`Tarifas actualizadas a modalidad: ${label}`, "fa-tags");
        });
    });

    // Inicializar URLs al cargar
    updateEnrollLinks(1, "AL MES", 1);
}

export function initPreEnrollment() {
    // La inscripción fluye directamente al wizard de contratación paso a paso estilo Smart Fit
}
