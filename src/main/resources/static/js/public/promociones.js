/**
 * public/promociones.js - Energy Gym Promotions & Offers
 * Responsabilidad: Contador regresivo de ofertas y modal de canje de cupones con copiado.
 */

import { openGymModal } from "../core/modal.js";
import { showGymToast } from "../core/toast.js";

export function initPromoCountdown() {
    const countdownEl = document.getElementById("promoCountdown");
    if (!countdownEl) return;

    let totalSeconds = 4 * 86400 + 11 * 3600 + 42 * 60 + 19;
    setInterval(() => {
        if (totalSeconds > 0) totalSeconds--;
        const d = Math.floor(totalSeconds / 86400);
        const h = Math.floor((totalSeconds % 86400) / 3600);
        const m = Math.floor((totalSeconds % 3600) / 60);
        const s = totalSeconds % 60;
        countdownEl.textContent = `${String(d).padStart(2, "0")}d : ${String(h).padStart(2, "0")}h : ${String(m).padStart(2, "0")}m : ${String(s).padStart(2, "0")}s`;
    }, 1000);
}

export function initPromoCoupons() {
    const promoBtns = document.querySelectorAll(".btn-promo-detail");
    if (promoBtns.length === 0) return;

    promoBtns.forEach((btn) => {
        btn.addEventListener("click", function () {
            const title = this.getAttribute("data-promo-title") || "PROMOCIÓN EXCLUSIVA";
            const code = this.getAttribute("data-promo-code") || "ENERGY";
            const desc = this.getAttribute("data-promo-desc") || "Descuento en planes y productos.";

            openGymModal(
                `<i class="fa-solid fa-ticket me-2"></i> ${title}`,
                `<div>
                    <p class="text-light text-sm mb-4">${desc}</p>
                    <div class="p-3 rounded bg-elevated border border-secondary d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <small class="text-muted text-2xs fw-bold d-block">CÓDIGO PROMOCIONAL</small>
                            <span class="font-anton fs-4 text-primary tracking-wider">${code}</span>
                        </div>
                        <button type="button" class="btn btn-primary py-2 px-3 text-xs" id="btnCopyPromoCode" data-code="${code}">
                            <i class="fa-regular fa-copy"></i> COPIAR CÓDIGO
                        </button>
                    </div>
                    <div class="d-flex gap-2">
                        <a href="/public/planes" class="btn btn-outline flex-grow-1">IR A PLANES</a>
                        <button type="button" class="btn btn-primary flex-grow-1" data-bs-dismiss="modal">CERRAR</button>
                    </div>
                </div>`
            );

            setTimeout(() => {
                const copyBtn = document.getElementById("btnCopyPromoCode");
                if (copyBtn) {
                    copyBtn.addEventListener("click", function () {
                        const c = this.getAttribute("data-code");
                        if (navigator.clipboard) {
                            navigator.clipboard.writeText(c).catch(() => {});
                        }
                        this.innerHTML = `<i class="fa-solid fa-check"></i> ¡COPIADO!`;
                        showGymToast(`Código ${c} copiado al portapapeles`, "fa-ticket");
                    });
                }
            }, 50);
        });
    });
}
