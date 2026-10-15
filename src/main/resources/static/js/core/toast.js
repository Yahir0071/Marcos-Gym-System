/**
 * core/toast.js - Energy Gym Toast Notification Engine
 * Responsabilidad: Gestión y renderizado de notificaciones flotantes temporales.
 */

export function showGymToast(message, iconClass = "fa-bolt") {
    let container = document.getElementById("gymToastContainer");
    if (!container) {
        container = document.createElement("div");
        container.id = "gymToastContainer";
        container.className = "gym-toast-container";
        document.body.appendChild(container);
    }

    const toast = document.createElement("div");
    toast.className = "gym-toast";
    toast.innerHTML = `
        <i class="fa-solid ${iconClass} text-primary fs-5"></i>
        <div class="flex-grow-1 text-xs fw-bold">${message}</div>
        <button type="button" class="btn-close btn-close-white ms-2" style="font-size:0.65rem;" aria-label="Cerrar"></button>
    `;
    container.appendChild(toast);

    const closeBtn = toast.querySelector(".btn-close");
    if (closeBtn) {
        closeBtn.addEventListener("click", () => toast.remove());
    }

    setTimeout(() => {
        if (toast.parentNode) {
            toast.remove();
        }
    }, 4000);
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
