/**
 * core/modal.js - Energy Gym Dynamic Modal Engine (DOM Seguro - C04 / M06)
 * Responsabilidad: Generación segura y accesible de ventanas modales sin interpolar datos de usuario en innerHTML.
 */

export function createDomElement(tag, className = "", text = null) {
    const el = document.createElement(tag);
    if (className) el.className = className;
    if (text !== null && text !== undefined) {
        el.textContent = String(text);
    }
    return el;
}

export function createIcon(iconClasses, ariaHidden = true) {
    const i = document.createElement("i");
    i.className = iconClasses;
    if (ariaHidden) {
        i.setAttribute("aria-hidden", "true");
    }
    return i;
}

/**
 * Abre el modal universal usando exclusivamente nodos DOM y texto plano (cero innerHTML).
 * Garantiza protección frente a XSS (C04) y soporte de teclado/foco accesible (M06).
 */
export function openSafeGymModal(titleText, bodyNode, iconClass = "fa-solid fa-bolt text-primary me-2") {
    const modalEl = document.getElementById("dynamicGymModal");
    if (!modalEl) return null;

    const titleEl = document.getElementById("dynamicGymModalTitle");
    const bodyEl = document.getElementById("dynamicGymModalBody");

    if (titleEl) {
        titleEl.replaceChildren();
        if (iconClass) {
            titleEl.appendChild(createIcon(iconClass));
        }
        titleEl.appendChild(document.createTextNode(String(titleText || "ENERGY GYM")));
    }

    if (bodyEl) {
        bodyEl.replaceChildren();
        if (bodyNode instanceof Node) {
            bodyEl.appendChild(bodyNode);
        } else if (bodyNode !== null && bodyNode !== undefined) {
            bodyEl.appendChild(createDomElement("p", "mb-0 text-light text-sm", String(bodyNode)));
        }
    }

    const triggerEl = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    if (triggerEl && !modalEl.contains(triggerEl)) {
        modalEl.addEventListener("hidden.bs.modal", () => {
            if (document.contains(triggerEl)) {
                triggerEl.focus();
            }
        }, { once: true });
    }

    if (window.bootstrap && window.bootstrap.Modal) {
        const modalInstance = window.bootstrap.Modal.getOrCreateInstance(modalEl);
        modalInstance.show();
        return modalInstance;
    }
    return null;
}

/**
 * Mantiene compatibilidad con módulos heredados (como QR Pass),
 * priorizando nodos DOM seguros cuando se reciben instancias de Node.
 */
export function openGymModal(title, bodyContent) {
    if (title instanceof Node || bodyContent instanceof Node) {
        const modalEl = document.getElementById("dynamicGymModal");
        if (!modalEl) return null;

        const titleEl = document.getElementById("dynamicGymModalTitle");
        const bodyEl = document.getElementById("dynamicGymModalBody");

        if (titleEl) {
            if (title instanceof Node) {
                titleEl.replaceChildren(title);
            } else {
                titleEl.textContent = String(title || "ENERGY GYM");
            }
        }
        if (bodyEl) {
            if (bodyContent instanceof Node) {
                bodyEl.replaceChildren(bodyContent);
            } else {
                bodyEl.textContent = String(bodyContent || "");
            }
        }
        if (window.bootstrap && window.bootstrap.Modal) {
            const modalInstance = window.bootstrap.Modal.getOrCreateInstance(modalEl);
            modalInstance.show();
            return modalInstance;
        }
        return null;
    }

    const modalEl = document.getElementById("dynamicGymModal");
    if (!modalEl) return null;

    const titleEl = document.getElementById("dynamicGymModalTitle");
    const bodyEl = document.getElementById("dynamicGymModalBody");

    if (titleEl) titleEl.innerHTML = title;
    if (bodyEl) bodyEl.innerHTML = bodyContent;

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
