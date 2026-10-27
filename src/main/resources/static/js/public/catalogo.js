/**
 * public/catalogo.js - Energy Gym Product Catalog & Reservation Dispatcher
 * Responsabilidad: Búsqueda, filtrado, ordenamiento de productos, detalle interactivo y emisión de reservas.
 */

import { postJson } from "../core/api-client.js";
import { openGymModal } from "../core/modal.js";
import { showGymToast } from "../core/toast.js";

export async function saveProductReservation(productName, price, qty, productId = 1) {
    let code = "#RES-" + Math.floor(10000 + Math.random() * 90000);
    const today = new Date().toLocaleDateString("es-PE", { day: "2-digit", month: "short", year: "numeric" });

    try {
        const resp = await postJson("/api/reservas", { productoId: productId, cantidad: qty });
        if (resp.ok) {
            const data = await resp.json();
            if (data.code) {
                code = data.code;
            }
        } else {
            const errData = await resp.json().catch(() => ({}));
            if (errData.error) {
                showGymToast(errData.error, "fa-circle-exclamation");
                return;
            }
        }
    } catch (err) {
        console.warn("Reserva offline / fallback local:", err);
    }

    const stored = JSON.parse(localStorage.getItem("energyGymReservas") || "[]");
    stored.unshift({
        code: code,
        date: today,
        product: productName,
        qty: qty,
        price: price,
        status: "PENDIENTE"
    });
    localStorage.setItem("energyGymReservas", JSON.stringify(stored));

    const sidebarCount = document.getElementById("sidebarReservaCount");
    if (sidebarCount) {
        const current = parseInt(sidebarCount.textContent, 10) || 0;
        sidebarCount.textContent = current + 1;
    }

    // Actualizar visualmente el stock en la tarjeta o detalle sin esperar recarga
    const detailStockEl = document.getElementById("detailStockCount");
    if (detailStockEl) {
        const currentStock = parseInt(detailStockEl.textContent, 10) || 0;
        detailStockEl.textContent = String(Math.max(0, currentStock - qty));
    }
    const cardBtn = document.querySelector(`.btn-quick-reserve[data-id="${productId}"]`);
    if (cardBtn) {
        const card = cardBtn.closest(".product-item-card");
        const badge = card?.querySelector(".product-stock-badge");
        if (badge) {
            const currentStock = parseInt(badge.getAttribute("data-stock") || "0", 10);
            const nextStock = Math.max(0, currentStock - qty);
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
            }
        }
    }

    openGymModal(
        `<i class="fa-solid fa-bag-shopping me-2"></i> ¡RESERVA CONFIRMADA!`,
        `<div class="text-center py-2">
            <i class="fa-solid fa-circle-check text-success mb-3" style="font-size: 3.2rem;"></i>
            <h4 class="font-anton text-light mb-1">${productName}</h4>
            <p class="text-muted text-xs mb-3">Cantidad reservada: <strong>${qty} unidad(es)</strong> · Total a pagar en local: <strong class="text-primary">S/ ${price * qty}</strong></p>
            <div class="p-3 rounded bg-elevated border border-primary mb-3">
                <small class="text-muted text-2xs d-block fw-bold">CÓDIGO DE RECOJO EN RECEPCIÓN</small>
                <span class="font-anton fs-3 text-primary tracking-wider">${code}</span>
            </div>
            <div class="d-flex gap-2">
                <a href="/cliente/mis-reservas" class="btn btn-primary flex-grow-1">IR A MIS RESERVAS</a>
                <button type="button" class="btn btn-outline flex-grow-1" data-bs-dismiss="modal">SEGUIR VIENDO</button>
            </div>
        </div>`
    );
    showGymToast(`Reserva ${code} creada para ${productName}`, "fa-bag-shopping");
}

export function initProductCatalog() {
    const productGrid = document.getElementById("publicProductGrid");
    if (!productGrid) return;

    const productSearch = document.getElementById("publicProductSearch");
    const productSort = document.getElementById("publicProductSort");
    const catBtns = document.querySelectorAll(".cat-filter-btn");

    let activeCategory = "ALL";

    // Actualizar conteo dinámico en botón TODOS
    const allBtn = document.querySelector('.cat-filter-btn[data-category="ALL"]');
    const allCards = Array.from(productGrid.querySelectorAll(".product-item-card"));
    if (allBtn && allCards.length > 0) {
        allBtn.textContent = `TODOS (${allCards.length})`;
    }

    // Contenedor de estado vacío cuando 0 productos coinciden
    let emptyState = document.getElementById("noProductsStateAlert");
    if (!emptyState) {
        emptyState = document.createElement("div");
        emptyState.id = "noProductsStateAlert";
        emptyState.className = "w-100 text-center py-5 my-4 d-none";
        emptyState.innerHTML = `
            <div class="p-4 rounded border border-secondary bg-card mx-auto" style="max-width: 480px;">
                <i class="fa-solid fa-box-open text-primary mb-3" style="font-size: 2.8rem;"></i>
                <h5 class="font-anton text-light mb-1">NO SE ENCONTRARON PRODUCTOS</h5>
                <p class="text-muted text-xs mb-3">No hay artículos que coincidan con los filtros o el texto ingresado.</p>
                <button type="button" class="btn btn-outline py-2 px-3 text-xs" id="btnResetProductFilters">
                    <i class="fa-solid fa-rotate-left me-1"></i> VER TODOS LOS PRODUCTOS
                </button>
            </div>
        `;
        productGrid.parentNode.insertBefore(emptyState, productGrid.nextSibling);

        const resetBtn = emptyState.querySelector("#btnResetProductFilters");
        if (resetBtn) {
            resetBtn.addEventListener("click", () => {
                if (productSearch) productSearch.value = "";
                if (productSort) productSort.value = "default";
                catBtns.forEach(b => {
                    b.classList.remove("btn-primary", "active");
                    b.classList.add("btn-outline");
                });
                if (allBtn) {
                    allBtn.classList.remove("btn-outline");
                    allBtn.classList.add("btn-primary", "active");
                }
                activeCategory = "ALL";
                filterAndSortProducts();
            });
        }
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
            const matchesCat = activeCategory === "ALL" || cat.includes(activeCategory);

            const isMatch = matchesSearch && matchesCat;
            if (isMatch) {
                // Se remueve d-none para garantizar visibilidad frente a reglas con !important de Bootstrap
                card.classList.remove("d-none");
                card.style.removeProperty("display");
                visibleCount++;
            } else {
                // Se añade d-none (display: none !important) para ocultar con prioridad total
                card.classList.add("d-none");
            }
        });

        // Alternar visualización del estado vacío
        if (emptyState) {
            if (visibleCount === 0 && cards.length > 0) {
                emptyState.classList.remove("d-none");
            } else {
                emptyState.classList.add("d-none");
            }
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
        btn.addEventListener("click", function () {
            catBtns.forEach((b) => {
                b.classList.remove("btn-primary", "active");
                b.classList.add("btn-outline");
            });
            this.classList.remove("btn-outline");
            this.classList.add("btn-primary", "active");
            activeCategory = this.getAttribute("data-category") || "ALL";
            filterAndSortProducts();
        });
    });

    document.querySelectorAll(".btn-quick-reserve").forEach((btn) => {
        btn.addEventListener("click", function () {
            const prod = this.getAttribute("data-product") || "Whey Protein 2 LB";
            const price = parseFloat(this.getAttribute("data-price")) || 149;
            const id = this.getAttribute("data-id") || 1;
            saveProductReservation(prod, price, 1, id);
        });
    });
}

export function initProductDetail() {
    const detailThumbs = document.querySelectorAll(".detail-thumb-btn");
    if (detailThumbs.length > 0) {
        detailThumbs.forEach((thumb) => {
            thumb.addEventListener("click", function () {
                detailThumbs.forEach((t) => t.classList.remove("active"));
                this.classList.add("active");
                const targetId = this.getAttribute("data-target");
                ["detailTabImage", "detailTabInfo", "detailTabTable"].forEach((id) => {
                    const pane = document.getElementById(id);
                    if (pane) {
                        if (id === targetId) pane.classList.remove("d-none");
                        else pane.classList.add("d-none");
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
            const unit = parseFloat(unitPriceEl.getAttribute("data-unit-price")) || 149;
            const qty = parseInt(qtyInput.value, 10) || 1;
            totalPriceEl.textContent = `S/ ${Math.round(unit * qty)}`;
        };
        if (btnQtyMinus) {
            btnQtyMinus.addEventListener("click", () => {
                let v = parseInt(qtyInput.value, 10) || 1;
                if (v > 1) {
                    qtyInput.value = v - 1;
                    updateSubtotal();
                }
            });
        }
        if (btnQtyPlus) {
            btnQtyPlus.addEventListener("click", () => {
                let v = parseInt(qtyInput.value, 10) || 1;
                if (v < 10) {
                    qtyInput.value = v + 1;
                    updateSubtotal();
                }
            });
        }
        if (btnConfirmReserve) {
            btnConfirmReserve.addEventListener("click", function () {
                const name = document.getElementById("detailName") ? document.getElementById("detailName").textContent.trim() : "Whey Protein 2 LB";
                const unit = parseFloat(unitPriceEl.getAttribute("data-unit-price")) || 149;
                const qty = parseInt(qtyInput.value, 10) || 1;
                const id = this.getAttribute("data-id") || 1;
                saveProductReservation(name, unit, qty, id);
            });
        }
    }
}
