/**
 * public/catalogo.js - Energy Gym Product Catalog & Reservation Dispatcher (DOM Seguro - C04)
 * Responsabilidad: Búsqueda, filtrado, ordenamiento de productos, detalle interactivo y emisión segura de reservas.
 */

import { postJson } from "../core/api-client.js";
import { openSafeGymModal, createDomElement, createIcon } from "../core/modal.js";
import { showGymToast } from "../core/toast.js";

async function saveProductReservation(button, qty) {
    const productId = Number(button.dataset.id);
    if (button.disabled || !Number.isSafeInteger(productId) || productId < 1 || !Number.isSafeInteger(qty) || qty < 1) return;
    button.disabled = true;
    let data;
    try {
        const resp = await postJson("/api/reservas", { productoId: productId, cantidad: qty });
        data = await resp.json().catch(() => ({}));
        if (!resp.ok || resp.redirected || !data.success || !data.code) {
            const errMsg = data.error || data.mensaje || "No se pudo confirmar la reserva. Verifica que tu sesión siga activa.";
            showGymToast(errMsg, "fa-circle-exclamation", "error");
            return;
        }
    } catch {
        showGymToast("Se perdió la conexión. Revisa Mis reservas antes de volver a intentar.", "fa-circle-exclamation", "error");
        return;
    } finally {
        button.disabled = false;
    }
    const { code, producto: productName, cantidad, total } = data;

    const sidebarCount = document.getElementById("sidebarReservaCount");
    if (sidebarCount) {
        const current = parseInt(sidebarCount.textContent, 10) || 0;
        sidebarCount.textContent = String(current + 1);
    }

    const detailStockEl = document.getElementById("detailStockCount");
    if (detailStockEl) {
        const currentStock = parseInt(detailStockEl.textContent, 10) || 0;
        const stock = Math.max(0, currentStock - cantidad);
        detailStockEl.textContent = String(stock);
        const qtyInput = document.getElementById("detailQtyInput");
        if (qtyInput) {
            qtyInput.max = String(stock);
            qtyInput.value = String(Math.min(Number(qtyInput.value), stock));
            qtyInput.dispatchEvent(new Event("change"));
        }
        if (stock === 0) {
            button.disabled = true;
            document.getElementById("btnAddToCartDetail")?.classList.add("d-none");
        }
    }

    const cardBtn = document.querySelector(`.btn-quick-reserve[data-id="${productId}"]`);
    if (cardBtn) {
        const card = cardBtn.closest(".product-item-card");
        const badge = card?.querySelector(".product-stock-badge");
        if (badge) {
            const currentStock = parseInt(badge.getAttribute("data-stock") || "0", 10);
            const nextStock = Math.max(0, currentStock - cantidad);
            badge.setAttribute("data-stock", String(nextStock));
            if (nextStock > 5) {
                badge.className = "badge text-success border border-success bg-transparent tracking-wide text-2xs product-stock-badge";
                badge.textContent = `DISPONIBLE · ${nextStock} UDS`;
            } else if (nextStock > 0) {
                badge.className = "badge text-warning border border-warning bg-transparent tracking-wide text-2xs product-stock-badge";
                badge.textContent = `ÚLTIMAS ${nextStock} UDS`;
            } else {
                badge.className = "badge text-danger border border-danger bg-transparent tracking-wide text-2xs product-stock-badge";
                badge.textContent = "AGOTADO";
                cardBtn.disabled = true;
                const add = card.querySelector(".btn-quick-add-cart");
                if (add) add.disabled = true;
            }
        }
    }

    const body = createDomElement("div", "text-center py-2");
    const checkIcon = createIcon("fa-solid fa-circle-check text-success mb-3");
    checkIcon.style.fontSize = "3.2rem";

    const titleEl = createDomElement("h4", "font-anton text-light mb-1", productName);
    const summaryEl = createDomElement("p", "text-muted text-xs mb-3");
    summaryEl.append(
        document.createTextNode("Cantidad reservada: "),
        createDomElement("strong", "", `${cantidad} unidad(es)`),
        document.createTextNode(" · Importe estimado en recepción: "),
        createDomElement("strong", "text-primary", `S/ ${Number(total).toFixed(2)}`)
    );

    const codeBox = createDomElement("div", "p-3 rounded bg-elevated border border-primary mb-3");
    codeBox.append(
        createDomElement("small", "text-muted text-2xs d-block fw-bold", "CÓDIGO DE RECOJO EN RECEPCIÓN"),
        createDomElement("span", "font-anton fs-3 text-primary tracking-wider", code)
    );

    const btnRow = createDomElement("div", "d-flex flex-wrap gap-2");
    const linkReservas = createDomElement("a", "btn btn-primary flex-grow-1", "IR A MIS RESERVAS");
    linkReservas.href = "/cliente/mis-reservas";
    const closeBtn = createDomElement("button", "btn btn-outline flex-grow-1", "SEGUIR VIENDO");
    closeBtn.type = "button";
    closeBtn.setAttribute("data-bs-dismiss", "modal");
    btnRow.append(linkReservas, closeBtn);

    body.append(checkIcon, titleEl, summaryEl, codeBox, btnRow);

    openSafeGymModal("Reserva registrada", body, "fa-solid fa-bag-shopping text-primary me-2");
    showGymToast(`Reserva ${code} creada para ${productName}`, "fa-bag-shopping");
}

export function initProductCatalog() {
    const productGrid = document.getElementById("publicProductGrid");
    if (!productGrid) return;

    const productSearch = document.getElementById("publicProductSearch");
    const productSort = document.getElementById("publicProductSort");
    const catBtns = document.querySelectorAll(".cat-filter-btn");

    let activeCategory = "ALL";

    const allBtn = document.querySelector('.cat-filter-btn[data-category="ALL"]');
    const allCards = Array.from(productGrid.querySelectorAll(".product-item-card"));
    if (allBtn && allCards.length > 0) {
        allBtn.textContent = `TODOS (${allCards.length})`;
    }

    let emptyState = document.getElementById("noProductsStateAlert");
    if (!emptyState) {
        emptyState = createDomElement("div", "w-100 text-center py-5 my-4 d-none");
        emptyState.id = "noProductsStateAlert";
        emptyState.setAttribute("role", "status");

        const card = createDomElement("div", "p-4 rounded border border-secondary bg-card mx-auto");
        card.style.maxWidth = "480px";
        const icon = createIcon("fa-solid fa-box-open text-primary mb-3");
        icon.style.fontSize = "2.8rem";
        const h5 = createDomElement("h5", "font-anton text-light mb-1", "NO SE ENCONTRARON PRODUCTOS");
        const p = createDomElement("p", "text-muted text-xs mb-3", "No hay artículos que coincidan con los filtros o el texto ingresado.");
        const resetBtn = createDomElement("button", "btn btn-outline py-2 px-3 text-xs");
        resetBtn.type = "button";
        resetBtn.id = "btnResetProductFilters";
        resetBtn.append(createIcon("fa-solid fa-rotate-left me-1"), document.createTextNode("VER TODOS LOS PRODUCTOS"));

        card.append(icon, h5, p, resetBtn);
        emptyState.appendChild(card);
        productGrid.parentNode.insertBefore(emptyState, productGrid.nextSibling);

        resetBtn.addEventListener("click", () => {
            if (productSearch) productSearch.value = "";
            if (productSort) productSort.value = "default";
            catBtns.forEach((b) => {
                b.classList.remove("btn-primary", "active");
                b.classList.add("btn-outline");
                b.setAttribute("aria-pressed", "false");
            });
            if (allBtn) {
                allBtn.classList.remove("btn-outline");
                allBtn.classList.add("btn-primary", "active");
                allBtn.setAttribute("aria-pressed", "true");
            }
            activeCategory = "ALL";
            filterAndSortProducts();
        });
    }

    const filterAndSortProducts = () => {
        const query = (productSearch ? productSearch.value : "").toLowerCase().trim();
        const sortMode = productSort ? productSort.value : "default";
        const cards = Array.from(productGrid.querySelectorAll(".product-item-card"));
        let visibleCount = 0;

        cards.forEach((card) => {
            const name = (card.getAttribute("data-name") || "").toLowerCase();
            const cat = (card.getAttribute("data-category") || "").toUpperCase();
            const matchesSearch = !query || name.includes(query) || cat.toLowerCase().includes(query);
            const matchesCat = activeCategory === "ALL" || cat === activeCategory;

            if (matchesSearch && matchesCat) {
                card.classList.remove("d-none");
                card.style.removeProperty("display");
                visibleCount++;
            } else {
                card.classList.add("d-none");
            }
        });

        if (emptyState) {
            emptyState.classList.toggle("d-none", !(visibleCount === 0 && cards.length > 0));
        }

        if (sortMode !== "default") {
            const sorted = cards.sort((a, b) => {
                const pa = parseFloat(a.getAttribute("data-price")) || 0;
                const pb = parseFloat(b.getAttribute("data-price")) || 0;
                return sortMode === "price-asc" ? pa - pb : pb - pa;
            });
            sorted.forEach((c) => productGrid.appendChild(c));
        }
    };

    if (productSearch) productSearch.addEventListener("input", filterAndSortProducts);
    if (productSort) productSort.addEventListener("change", filterAndSortProducts);

    catBtns.forEach((btn) => {
        btn.setAttribute("aria-pressed", btn.classList.contains("active") ? "true" : "false");
        btn.addEventListener("click", function () {
            catBtns.forEach((b) => {
                b.classList.remove("btn-primary", "active");
                b.classList.add("btn-outline");
                b.setAttribute("aria-pressed", "false");
            });
            this.classList.remove("btn-outline");
            this.classList.add("btn-primary", "active");
            this.setAttribute("aria-pressed", "true");
            activeCategory = this.getAttribute("data-category") || "ALL";
            filterAndSortProducts();
        });
    });

    document.querySelectorAll(".btn-quick-reserve").forEach((btn) => {
        btn.addEventListener("click", () => saveProductReservation(btn, 1));
    });
}

export function initProductDetail() {
    const detailThumbs = document.querySelectorAll(".detail-thumb-btn");
    if (detailThumbs.length > 0) {
        detailThumbs.forEach((thumb) => {
            thumb.addEventListener("click", function () {
                detailThumbs.forEach((t) => {
                    t.classList.remove("active");
                    t.setAttribute("aria-pressed", "false");
                });
                this.classList.add("active");
                this.setAttribute("aria-pressed", "true");
                const targetId = this.getAttribute("data-target");
                ["detailTabImage", "detailTabInfo", "detailTabTable"].forEach((id) => {
                    const pane = document.getElementById(id);
                    if (pane) {
                        pane.classList.toggle("d-none", id !== targetId);
                    }
                });
            });
        });
    }

    const qtyInput = document.getElementById("detailQtyInput");
    const btnQtyMinus = document.getElementById("btnQtyMinus");
    const btnQtyPlus = document.getElementById("btnQtyPlus");
    const unitPriceEl = document.getElementById("detailUnitPrice");
    const totalPriceEl = document.getElementById("detailTotalPrice");
    const btnConfirmReserve = document.getElementById("btnConfirmProductReserve");

    if (qtyInput && unitPriceEl && totalPriceEl) {
        const updateSubtotal = () => {
            const unit = Number(unitPriceEl.dataset.unitPrice);
            const qty = Number(qtyInput.value);
            totalPriceEl.textContent = `S/ ${(unit * qty).toFixed(2)}`;
        };
        qtyInput.addEventListener("change", updateSubtotal);
        if (btnQtyMinus) {
            btnQtyMinus.addEventListener("click", () => {
                const v = parseInt(qtyInput.value, 10) || 1;
                if (v > 1) {
                    qtyInput.value = String(v - 1);
                    updateSubtotal();
                }
            });
        }
        if (btnQtyPlus) {
            btnQtyPlus.addEventListener("click", () => {
                const v = parseInt(qtyInput.value, 10) || 1;
                if (v < Number(qtyInput.max)) {
                    qtyInput.value = String(v + 1);
                    updateSubtotal();
                }
            });
        }
        if (btnConfirmReserve) {
            btnConfirmReserve.addEventListener("click", () => saveProductReservation(btnConfirmReserve, Number(qtyInput.value)));
        }
    }
}
