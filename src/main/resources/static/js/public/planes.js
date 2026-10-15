/**
 * public/planes.js - Energy Gym Membership Plans Engine
 * Responsabilidad: Selector dinámico de periodos de facturación y pre-inscripción online.
 */

import { openGymModal } from "../core/modal.js";
import { showGymToast } from "../core/toast.js";

export function initBillingSwitcher() {
    const billingBtns = document.querySelectorAll(".billing-period-btn");
    if (billingBtns.length === 0) return;

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
            showGymToast(`Tarifas actualizadas a modalidad: ${label}`, "fa-tags");
        });
    });
}

export function initPreEnrollment() {
    const chooseBtns = document.querySelectorAll(".btn-choose-plan");
    if (chooseBtns.length === 0) return;

    chooseBtns.forEach((btn) => {
        btn.addEventListener("click", function () {
            const planName = this.getAttribute("data-plan-name") || "Plan Plus";
            const planPrice = this.getAttribute("data-plan-price") || "119";
            const code = "PRE-" + Math.floor(1000 + Math.random() * 9000);

            openGymModal(
                `<i class="fa-solid fa-bolt me-2"></i> PRE-INSCRIPCIÓN: ${planName.toUpperCase()}`,
                `<form id="preEnrollForm">
                    <div class="p-3 rounded bg-elevated border border-secondary mb-3 d-flex justify-content-between align-items-center">
                        <div>
                            <div class="text-2xs text-muted fw-bold">PLAN SELECCIONADO</div>
                            <div class="font-anton fs-5 text-primary">${planName}</div>
                        </div>
                        <div class="text-end">
                            <div class="text-2xs text-muted fw-bold">TARIFA BASE</div>
                            <div class="font-anton fs-4 text-light">S/ ${planPrice} / mes</div>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">NOMBRE COMPLETO</label>
                        <input type="text" class="form-control" id="preName" placeholder="Ej. Carlos Mendoza" required>
                    </div>
                    <div class="row g-2 mb-3">
                        <div class="col-6">
                            <label class="form-label">DNI / DOCUMENTO</label>
                            <input type="text" class="form-control" placeholder="72563123" maxlength="8" required>
                        </div>
                        <div class="col-6">
                            <label class="form-label">TELÉFONO</label>
                            <input type="text" class="form-control" placeholder="987 654 321" required>
                        </div>
                    </div>
                    <div class="mb-4">
                        <label class="form-label">CUPÓN PROMOCIONAL (OPCIONAL)</label>
                        <input type="text" class="form-control" placeholder="Ej. ENERGY2X1 o SHAKERPRO">
                    </div>
                    <button type="submit" class="btn btn-primary w-100 py-3">
                        GENERAR CÓDIGO DE RESERVA DE PLAN
                    </button>
                </form>`
            );

            setTimeout(() => {
                const form = document.getElementById("preEnrollForm");
                if (form) {
                    form.addEventListener("submit", function (ev) {
                        ev.preventDefault();
                        const name = document.getElementById("preName")?.value || "Futuro Socio";
                        const modalBody = document.getElementById("dynamicGymModalBody");
                        if (modalBody) {
                            modalBody.innerHTML = `
                                <div class="text-center py-3">
                                    <i class="fa-solid fa-circle-check text-success" style="font-size: 3.5rem;"></i>
                                    <h4 class="font-anton text-light mt-3 mb-1">¡CUPÓN DE INSCRIPCIÓN GENERADO!</h4>
                                    <p class="text-muted text-xs mb-3">Hola <strong>${name}</strong>, presenta este código en la recepción de Energy Gym para activar tu <strong>${planName}</strong>:</p>
                                    <div class="p-3 rounded bg-elevated border border-primary font-anton fs-3 text-primary tracking-wider mb-4">
                                        #${code}
                                    </div>
                                    <button type="button" class="btn btn-primary w-100" data-bs-dismiss="modal">ENTENDIDO</button>
                                </div>
                            `;
                        }
                        showGymToast(`Pre-inscripción #${code} generada con éxito`, "fa-check");
                    });
                }
            }, 50);
        });
    });
}
