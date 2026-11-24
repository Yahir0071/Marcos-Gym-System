/**
 * core/toast.js - Energy Gym Toast Notification Engine (DOM Seguro - C04 / M06)
 * Responsabilidad: Gestión y renderizado seguro y accesible de notificaciones flotantes temporales.
 */

const SAFE_ICON_PATTERN = /^[a-z0-9-\s]+$/i;

export function showGymToast(message, iconClass = "fa-bolt", variant = "info") {
    let container = document.getElementById("gymToastContainer");
    if (!container) {
        container = document.createElement("div");
        container.id = "gymToastContainer";
        container.className = "gym-toast-container";
        container.setAttribute("aria-live", "polite");
        container.setAttribute("aria-atomic", "true");
        document.body.appendChild(container);
    }

    const toast = document.createElement("div");
    toast.className = "gym-toast";
    toast.setAttribute("role", variant === "error" ? "alert" : "status");
    if (variant === "error") {
        toast.classList.add("gym-toast-error");
    }

    const icon = document.createElement("i");
    const sanitizedIcon = SAFE_ICON_PATTERN.test(String(iconClass)) ? String(iconClass) : "fa-bolt";
    const iconColor = variant === "error" ? "text-danger" : "text-primary";
    icon.className = `fa-solid ${sanitizedIcon} ${iconColor} fs-5`;
    icon.setAttribute("aria-hidden", "true");

    const textBox = document.createElement("div");
    textBox.className = "flex-grow-1 text-xs fw-bold";
    textBox.textContent = String(message ?? "");

    const closeBtn = document.createElement("button");
    closeBtn.type = "button";
    closeBtn.className = "btn-close btn-close-white ms-2";
    closeBtn.style.fontSize = "0.65rem";
    closeBtn.setAttribute("aria-label", "Cerrar notificación");
    closeBtn.addEventListener("click", () => toast.remove());

    toast.append(icon, textBox, closeBtn);
    container.appendChild(toast);

    setTimeout(() => {
        if (toast.parentNode) {
            toast.remove();
        }
    }, 4500);
}

export function initDataToastTriggers() {
    document.querySelectorAll("[data-toast]").forEach((el) => {
        el.addEventListener("click", function (e) {
            e.preventDefault();
            const msg = this.getAttribute("data-toast");
            if (msg) {
                showGymToast(msg, "fa-circle-check");
            }
        });
    });
}
