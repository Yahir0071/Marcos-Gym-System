/**
 * public/promociones.js - Energy Gym Promotions & Offers (DOM Seguro - C04)
 * Responsabilidad: Contador regresivo de ofertas y modal de canje de cupones sin innerHTML.
 */

import { openSafeGymModal, createDomElement, createIcon } from "../core/modal.js";
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

            const body = createDomElement("div");
            body.appendChild(createDomElement("p", "text-light text-sm mb-4", desc));

            const codeCard = createDomElement(
                "div",
                "p-3 rounded bg-elevated border border-secondary d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4"
            );
            const codeInfo = createDomElement("div");
            codeInfo.append(
                createDomElement("small", "text-muted text-2xs fw-bold d-block", "CÓDIGO PROMOCIONAL"),
                createDomElement("span", "font-anton fs-4 text-primary tracking-wider", code)
            );

            const copyBtn = createDomElement("button", "btn btn-primary py-2 px-3 text-xs");
            copyBtn.type = "button";
            copyBtn.id = "btnCopyPromoCode";
            copyBtn.append(createIcon("fa-regular fa-copy me-1"), document.createTextNode("COPIAR CÓDIGO"));
            copyBtn.addEventListener("click", function () {
                if (navigator.clipboard) {
                    navigator.clipboard.writeText(code).catch(() => {});
                }
                this.replaceChildren(createIcon("fa-solid fa-check me-1"), document.createTextNode("¡COPIADO!"));
                showGymToast(`Código ${code} copiado al portapapeles`, "fa-ticket");
            });

            codeCard.append(codeInfo, copyBtn);

            const actions = createDomElement("div", "d-flex flex-wrap gap-2");
            const planesLink = createDomElement("a", "btn btn-outline flex-grow-1", "IR A PLANES");
            planesLink.href = "/planes";
            const closeBtn = createDomElement("button", "btn btn-primary flex-grow-1", "CERRAR");
            closeBtn.type = "button";
            closeBtn.setAttribute("data-bs-dismiss", "modal");
            actions.append(planesLink, closeBtn);

            body.append(codeCard, actions);

            openSafeGymModal(title, body, "fa-solid fa-ticket text-primary me-2");
        });
    });
}
