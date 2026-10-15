/**
 * core/modal.js - Energy Gym Dynamic Modal Engine
 * Responsabilidad: Generación y apertura de ventanas modales dinámicas e interactivas.
 */

export function openGymModal(title, bodyHtml) {
    const modalEl = document.getElementById("dynamicGymModal");
    if (!modalEl) return null;

    const titleEl = document.getElementById("dynamicGymModalTitle");
    const bodyEl = document.getElementById("dynamicGymModalBody");

    if (titleEl) titleEl.innerHTML = title;
    if (bodyEl) bodyEl.innerHTML = bodyHtml;

    if (window.bootstrap && window.bootstrap.Modal) {
        const modalInstance = window.bootstrap.Modal.getOrCreateInstance(modalEl);
        modalInstance.show();
        return modalInstance;
    }
    return null;
}

export function closeGymModal() {
    const modalEl = document.getElementById("dynamicGymModal");
    if (modalEl && window.bootstrap && window.bootstrap.Modal) {
        const modalInstance = window.bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) {
            modalInstance.hide();
        }
    }
}
