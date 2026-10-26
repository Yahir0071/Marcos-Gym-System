/**
 * cliente/portal.js - Energy Gym Client Portal Logic
 * Responsabilidad: Seguimiento de entrenamientos, renovación, congelamiento, vouchers y gestión de avisos.
 */

import { openGymModal } from "../core/modal.js";
import { showGymToast } from "../core/toast.js";

export function initWorkoutTracker() {
    const workoutBtns = document.querySelectorAll(".workout-day-btn");
    if (workoutBtns.length === 0) return;

    const updateWorkoutTrackerSummary = () => {
        const activeCount = document.querySelectorAll(".workout-day-btn.active").length;
        const summary = document.getElementById("weeklyTrackerSummary");
        if (summary) summary.textContent = `${activeCount} / 6 DÍAS COMPLETADOS`;
        const att = document.getElementById("memberAttendanceCount");
        if (att) att.textContent = `${13 + activeCount} visitas`;
        const streak = document.getElementById("topbarStreakCount");
        if (streak) streak.textContent = `${7 + activeCount} DÍAS 🔥`;
    };

    workoutBtns.forEach((btn) => {
        btn.addEventListener("click", function () {
            this.classList.toggle("active");
            const icon = this.querySelector("i");
            if (this.classList.contains("active")) {
                if (icon) icon.className = "fa-solid fa-circle-check fs-4 text-primary";
                showGymToast(`¡Entrenamiento del día ${this.getAttribute("data-day")} registrado!`, "fa-dumbbell");
            } else {
                if (icon) icon.className = "fa-regular fa-circle fs-4 text-muted";
            }
            updateWorkoutTrackerSummary();
        });
    });

    const btnCheckinToday = document.getElementById("btnCheckinToday");
    if (btnCheckinToday) {
        btnCheckinToday.addEventListener("click", function () {
            const todayBtn = document.getElementById("todayWorkoutBtn");
            if (todayBtn && !todayBtn.classList.contains("active")) {
                todayBtn.click();
            } else {
                showGymToast("¡Tu asistencia de hoy ya está registrada! Sigue así 🔥", "fa-fire");
            }
            this.innerHTML = `<i class="fa-solid fa-check"></i> ENTRENO DE HOY REGISTRADO`;
        });
    }
}

export function initPlanActions() {
    const btnRenewPlan = document.getElementById("btnRenewPlan");
    if (btnRenewPlan) {
        btnRenewPlan.addEventListener("click", function (e) {
            // If it's a direct link to planes, allow standard navigation
            if (this.getAttribute("href")) return;
            e.preventDefault();

            openGymModal(
                `<i class="fa-solid fa-bolt me-2"></i> RENOVAR O MEJORAR TU MEMBRESÍA`,
                `<div>
                    <p class="text-muted text-xs mb-3">Selecciona el plan con el que deseas continuar tu próximo ciclo:</p>
                    <div class="d-flex flex-column gap-2 mb-4">
                        <label class="p-3 rounded bg-elevated border border-primary d-flex justify-content-between align-items-center cursor-pointer">
                            <div>
                                <input type="radio" name="renewChoice" value="PLUS" checked class="me-2">
                                <strong class="font-anton text-primary">MANTENER PLAN PLUS (30 DÍAS)</strong>
                            </div>
                            <span class="font-anton text-light">S/ 119</span>
                        </label>
                        <label class="p-3 rounded bg-elevated border border-secondary d-flex justify-content-between align-items-center cursor-pointer">
                            <div>
                                <input type="radio" name="renewChoice" value="PREMIUM" class="me-2">
                                <strong class="font-anton text-light">MEJORAR A PLAN PREMIUM VIP (30 DÍAS)</strong>
                            </div>
                            <span class="font-anton text-primary">S/ 159</span>
                        </label>
                    </div>
                    <button type="button" class="btn btn-primary w-100 py-3" id="btnConfirmRenewalModal">
                        CONFIRMAR SOLICITUD DE RENOVACIÓN
                    </button>
                </div>`
            );

            setTimeout(() => {
                const confirmBtn = document.getElementById("btnConfirmRenewalModal");
                if (confirmBtn) {
                    confirmBtn.addEventListener("click", () => {
                        const selected = document.querySelector('input[name="renewChoice"]:checked')?.value || "PLUS";
                        const planTitle = document.getElementById("memberPlanTitle");
                        const endDate = document.getElementById("memberEndDate");
                        const daysLeft = document.getElementById("memberDaysLeft");
                        const progress = document.getElementById("memberDaysProgress");

                        if (planTitle) planTitle.textContent = selected;
                        if (endDate) endDate.textContent = "02 Mar 2025";
                        if (daysLeft) daysLeft.textContent = "34 días";
                        if (progress) progress.style.width = "100%";

                        const card = document.getElementById("renewalAlertCard");
                        if (card) {
                            card.style.borderTop = "3px solid var(--accent-success)";
                            card.style.boxShadow = "none";
                        }
                        const icon = document.getElementById("renewalAlertIcon");
                        if (icon) icon.className = "fa-solid fa-circle-check text-success fs-5";
                        const titleEl = document.getElementById("renewalAlertTitle");
                        if (titleEl) {
                            titleEl.className = "font-anton text-success m-0";
                            titleEl.textContent = "MEMBRESÍA RENOVADA CON ÉXITO";
                        }
                        const descEl = document.getElementById("renewalAlertDesc");
                        if (descEl) {
                            descEl.innerHTML = `Tu <strong>Plan ${selected}</strong> ha sido extendido por 30 días adicionales. Ahora cuentas con <strong class="text-success">34 días restantes</strong>.`;
                        }

                        if (window.bootstrap && window.bootstrap.Modal) {
                            const modal = window.bootstrap.Modal.getInstance(document.getElementById("dynamicGymModal"));
                            if (modal) modal.hide();
                        }
                        showGymToast(`¡Membresía Plan ${selected} renovada por 30 días adicionales!`, "fa-id-card");
                    });
                }
            }, 50);
        });
    }

    const btnFreezePlan = document.getElementById("btnFreezePlan");
    if (btnFreezePlan) {
        btnFreezePlan.addEventListener("click", () => {
            openGymModal(
                `<i class="fa-regular fa-snowflake me-2"></i> CONGELAMIENTO DE MEMBRESÍA`,
                `<div>
                    <p class="text-muted text-xs mb-3">Puedes pausar los días de tu membresía por viaje o descanso médico hasta por 15 días sin perder tu vigencia.</p>
                    <div class="mb-3">
                        <label class="form-label">CANTIDAD DE DÍAS A CONGELAR</label>
                        <select class="form-select" id="freezeDaysSelect">
                            <option value="7">7 días</option>
                            <option value="15">15 días</option>
                        </select>
                    </div>
                    <button type="button" class="btn btn-primary w-100" data-bs-dismiss="modal" id="btnSubmitFreeze">
                        SOLICITAR CONGELAMIENTO
                    </button>
                </div>`
            );

            setTimeout(() => {
                const submitBtn = document.getElementById("btnSubmitFreeze");
                if (submitBtn) {
                    submitBtn.addEventListener("click", () => {
                        showGymToast("Solicitud de congelamiento registrada correctamente", "fa-snowflake");
                    });
                }
            }, 50);
        });
    }
}

export function initReservasClient() {
    const bindButtons = () => {
        document.querySelectorAll(".btn-view-voucher").forEach((btn) => {
            if (btn.dataset.bound) return;
            btn.dataset.bound = "true";
            btn.addEventListener("click", function () {
                const code = this.getAttribute("data-code") || "#RES-00001";
                const prod = this.getAttribute("data-product") || "Suplemento";
                const qty = this.getAttribute("data-qty") || "1";
                const status = this.getAttribute("data-status") || "PENDIENTE";
                const socio = this.getAttribute("data-socio") || "Socio Energy Gym";

                openGymModal(
                    `<i class="fa-solid fa-receipt me-2"></i> VOUCHER DE RESERVA ${code}`,
                    `<div class="p-2">
                        <div class="p-3 rounded bg-elevated border border-secondary mb-3">
                            <div class="d-flex justify-content-between mb-2">
                                <span class="text-muted text-xs">CÓDIGO:</span>
                                <strong class="font-anton text-primary">${code}</strong>
                            </div>
                            <div class="d-flex justify-content-between mb-2">
                                <span class="text-muted text-xs">SOCIO:</span>
                                <strong class="text-light text-xs">${socio}</strong>
                            </div>
                            <div class="d-flex justify-content-between mb-2">
                                <span class="text-muted text-xs">PRODUCTO:</span>
                                <strong class="text-light text-xs">${prod} (x${qty})</strong>
                            </div>
                            <div class="d-flex justify-content-between">
                                <span class="text-muted text-xs">ESTADO ACTUAL:</span>
                                <span class="badge bg-primary text-dark font-anton">${status}</span>
                            </div>
                        </div>
                        <p class="text-muted text-2xs mb-3">Presenta este voucher en el counter de suplementos de Energy Gym para recoger tu pedido.</p>
                        <button type="button" class="btn btn-primary w-100" data-bs-dismiss="modal">CERRAR VOUCHER</button>
                    </div>`
                );
            });
        });

        document.querySelectorAll(".btn-cancel-reserva").forEach((btn) => {
            if (btn.dataset.bound) return;
            btn.dataset.bound = "true";
            btn.addEventListener("click", function () {
                const row = this.closest("tr");
                const statusCell = row?.querySelector("td:nth-child(5)");
                if (statusCell) {
                    statusCell.innerHTML = `<span class="status-inactive">CANCELADA</span>`;
                }
                this.remove();
                showGymToast("Reserva cancelada correctamente", "fa-ban");
            });
        });
    };

    if (document.getElementById("misReservasTbody")) {
        bindButtons();
    }
}

export function initNotifications() {
    const notifFilterBtns = document.querySelectorAll(".notif-filter-btn");
    const notifCards = document.querySelectorAll(".notif-card-item");
    if (notifFilterBtns.length > 0) {
        notifFilterBtns.forEach((btn) => {
            btn.addEventListener("click", function () {
                notifFilterBtns.forEach((b) => {
                    b.classList.remove("btn-primary", "active");
                    b.classList.add("btn-outline");
                });
                this.classList.remove("btn-outline");
                this.classList.add("btn-primary", "active");
                const cat = this.getAttribute("data-notif-filter");
                notifCards.forEach((card) => {
                    const c = card.getAttribute("data-notif-cat");
                    card.style.display = cat === "ALL" || c === cat ? "" : "none";
                });
            });
        });
    }

    const btnMarkAllRead = document.getElementById("btnMarkAllRead");
    if (btnMarkAllRead) {
        btnMarkAllRead.addEventListener("click", () => {
            document.querySelectorAll(".notif-unread-badge").forEach((b) => b.remove());
            const badge = document.getElementById("sidebarNotifCount");
            if (badge) badge.textContent = "0";
            showGymToast("Todas las notificaciones marcadas como leídas", "fa-check-double");
        });
    }

    document.querySelectorAll(".btn-dismiss-notif").forEach((btn) => {
        btn.addEventListener("click", function () {
            const card = this.closest(".notif-card-item");
            if (card) card.remove();
            showGymToast("Notificación archivada", "fa-box-archive");
        });
    });
}

export function initCartAndCompras() {
    const DEFAULT_CART_ITEMS = [
        {
            id: "1",
            name: "Whey Protein 2 LB",
            category: "Suplementos",
            img: "/img/whey.jpg",
            price: 149.0,
            qty: 1,
            sub: 149.0
        },
        {
            id: "2",
            name: "Shaker 700ml",
            category: "Botellas",
            img: "/img/shaker.jpg",
            price: 45.0,
            qty: 1,
            sub: 45.0
        }
    ];

    const getStoredCartItems = () => {
        try {
            const raw = sessionStorage.getItem("energyGymCartItems");
            if (raw) {
                const parsed = JSON.parse(raw);
                if (Array.isArray(parsed)) return parsed;
            }
        } catch (e) {
            // ignore
        }
        return DEFAULT_CART_ITEMS;
    };

    const buildCartRowHtml = (item, isFirst) => `
        <div class="p-4 ${!isFirst ? "border-top border-secondary" : ""} cart-item-row"
             data-id="${item.id}" data-name="${item.name}" data-category="${item.category}" data-price="${Number(item.price).toFixed(2)}" data-img="${item.img}">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="d-flex align-items-center justify-content-center border border-secondary bg-deep-dark rounded overflow-hidden p-1" style="width: 80px; height: 80px; flex-shrink: 0;">
                        <img src="${item.img}" alt="${item.name}" class="img-fluid h-100 obj-contain">
                    </div>
                    <div>
                        <div class="d-flex align-items-center gap-2 mb-1">
                            <span class="badge bg-dark text-primary border border-secondary font-anton text-2xs">${String(item.category).toUpperCase()}</span>
                            <span class="text-success text-2xs fw-bold"><i class="fa-solid fa-circle-check me-1"></i>En stock para recojo hoy</span>
                        </div>
                        <h5 class="font-anton text-light mb-1">${item.name}</h5>
                        <div class="font-anton text-muted text-xs">Precio unitario: <span class="text-light">S/ ${Number(item.price).toFixed(2)}</span></div>
                    </div>
                </div>
                <div class="d-flex align-items-center gap-4 ms-auto">
                    <div class="d-flex align-items-center border border-secondary bg-deep-dark rounded">
                        <button type="button" class="btn btn-link text-light text-decoration-none px-3 py-1 font-anton cart-qty-minus" title="Disminuir cantidad">-</button>
                        <span class="px-3 font-anton text-light text-sm cart-item-qty">${item.qty}</span>
                        <button type="button" class="btn btn-link text-light text-decoration-none px-3 py-1 font-anton cart-qty-plus" title="Aumentar cantidad">+</button>
                    </div>
                    <div class="text-end" style="min-width: 100px;">
                        <div class="font-anton text-primary fs-5 cart-item-subtotal">S/ ${(Number(item.price) * Number(item.qty)).toFixed(2)}</div>
                        <button type="button" class="btn btn-link p-0 text-danger text-decoration-none font-anton text-2xs cart-item-remove">
                            <i class="fa-regular fa-trash-can me-1"></i>ELIMINAR
                        </button>
                    </div>
                </div>
            </div>
        </div>`;

    const recalculateCart = () => {
        const rows = document.querySelectorAll(".cart-item-row");
        let totalItems = 0;
        let grandTotal = 0;
        const storedItems = [];

        rows.forEach((row) => {
            const id = row.getAttribute("data-id") || "1";
            const name = row.getAttribute("data-name") || "Producto";
            const category = row.getAttribute("data-category") || "Suplementos";
            const img = row.getAttribute("data-img") || "/img/whey.jpg";
            const price = parseFloat(row.getAttribute("data-price") || "0");
            const qtyEl = row.querySelector(".cart-item-qty");
            const subEl = row.querySelector(".cart-item-subtotal");
            const qty = parseInt(qtyEl ? qtyEl.textContent.trim() : "1", 10) || 1;
            const sub = price * qty;
            totalItems += qty;
            grandTotal += sub;
            if (subEl) subEl.textContent = `S/ ${sub.toFixed(2)}`;
            storedItems.push({ id, name, category, img, price, qty, sub });
        });

        const baseImponible = grandTotal / 1.18;
        const igv = grandTotal - baseImponible;

        const counterEl = document.getElementById("cartHeaderCounter");
        if (counterEl) {
            counterEl.innerHTML = `<i class="fa-solid fa-cart-shopping me-1"></i> CARRITO (${totalItems} ${totalItems === 1 ? "PRODUCTO" : "PRODUCTOS"})`;
        }
        const sumBase = document.getElementById("cartSummaryBase");
        if (sumBase) sumBase.textContent = `S/ ${baseImponible.toFixed(2)}`;
        const sumIgv = document.getElementById("cartSummaryIgv");
        if (sumIgv) sumIgv.textContent = `S/ ${igv.toFixed(2)}`;
        const sumSub = document.getElementById("cartSummarySubtotal");
        if (sumSub) sumSub.textContent = `S/ ${grandTotal.toFixed(2)}`;
        const sumTot = document.getElementById("cartSummaryTotal");
        if (sumTot) sumTot.textContent = `S/ ${grandTotal.toFixed(2)}`;

        try {
            sessionStorage.setItem("energyGymCartItems", JSON.stringify(storedItems));
            sessionStorage.setItem("energyGymCartTotal", grandTotal.toFixed(2));
        } catch (e) {
            // ignore storage errors
        }
    };

    const bindRowEvents = (row) => {
        const minusBtn = row.querySelector(".cart-qty-minus");
        const plusBtn = row.querySelector(".cart-qty-plus");
        const removeBtn = row.querySelector(".cart-item-remove");

        if (minusBtn && !minusBtn.dataset.bound) {
            minusBtn.dataset.bound = "true";
            minusBtn.addEventListener("click", () => {
                const qtyEl = row.querySelector(".cart-item-qty");
                if (!qtyEl) return;
                let qty = parseInt(qtyEl.textContent.trim(), 10) || 1;
                if (qty > 1) {
                    qty--;
                    qtyEl.textContent = String(qty);
                    recalculateCart();
                }
            });
        }

        if (plusBtn && !plusBtn.dataset.bound) {
            plusBtn.dataset.bound = "true";
            plusBtn.addEventListener("click", () => {
                const qtyEl = row.querySelector(".cart-item-qty");
                if (!qtyEl) return;
                let qty = parseInt(qtyEl.textContent.trim(), 10) || 1;
                qty++;
                qtyEl.textContent = String(qty);
                recalculateCart();
            });
        }

        if (removeBtn && !removeBtn.dataset.bound) {
            removeBtn.dataset.bound = "true";
            removeBtn.addEventListener("click", () => {
                row.remove();
                recalculateCart();
                showGymToast("Producto eliminado del carrito", "fa-trash-can");
            });
        }
    };

    // 1. Si estamos en /cliente/carrito, restaurar los ítems guardados en sessionStorage
    const cartContainer = document.getElementById("cartItemsContainer");
    if (cartContainer) {
        const saved = sessionStorage.getItem("energyGymCartItems");
        if (saved) {
            const parsed = getStoredCartItems();
            cartContainer.innerHTML = parsed.map((it, idx) => buildCartRowHtml(it, idx === 0)).join("");
        }
        cartContainer.querySelectorAll(".cart-item-row").forEach(bindRowEvents);
        recalculateCart();
    }

    // Actualizar badge de cantidad en /cliente/productos
    const headerCartBadge = document.getElementById("headerCartBadge");
    if (headerCartBadge) {
        const items = getStoredCartItems();
        const totalQty = items.reduce((acc, it) => acc + (Number(it.qty) || 0), 0);
        headerCartBadge.textContent = String(totalQty);
    }

    // Agregar al carrito desde /cliente/detalle-producto (#btnAddToCartDetail)
    const btnAddToCartDetail = document.getElementById("btnAddToCartDetail");
    if (btnAddToCartDetail) {
        btnAddToCartDetail.addEventListener("click", function (e) {
            e.preventDefault();
            const id = String(this.getAttribute("data-id") || "1");
            const name = this.getAttribute("data-name") || "Whey Protein 2 LB";
            const price = parseFloat(this.getAttribute("data-price") || "149.00");
            const img = this.getAttribute("data-img") || "/img/whey.jpg";
            const category = this.getAttribute("data-category") || "Suplementos";
            const qtyInput = document.getElementById("detailQtyInput");
            const addQty = parseInt(qtyInput ? qtyInput.value : "1", 10) || 1;

            const items = getStoredCartItems();
            const found = items.find((i) => String(i.id) === id || i.name === name);
            if (found) {
                found.qty = (Number(found.qty) || 1) + addQty;
                found.sub = found.qty * found.price;
            } else {
                items.push({
                    id,
                    name,
                    category,
                    img,
                    price,
                    qty: addQty,
                    sub: price * addQty
                });
            }
            const newTotal = items.reduce((acc, it) => acc + Number(it.sub), 0);
            try {
                sessionStorage.setItem("energyGymCartItems", JSON.stringify(items));
                sessionStorage.setItem("energyGymCartTotal", newTotal.toFixed(2));
            } catch (err) {
                // ignore
            }
            window.location.href = this.getAttribute("href") || "/cliente/carrito";
        });
    }

    // Botón para agregar rápidamente Creatina desde la recomendación en Carrito
    const btnUpsell = document.getElementById("btnAddUpsellCreatina");
    if (btnUpsell) {
        btnUpsell.addEventListener("click", function () {
            const container = document.getElementById("cartItemsContainer");
            if (!container) return;
            const existing = container.querySelector('.cart-item-row[data-id="3"]');
            if (existing) {
                const qtyEl = existing.querySelector(".cart-item-qty");
                if (qtyEl) qtyEl.textContent = String((parseInt(qtyEl.textContent.trim(), 10) || 1) + 1);
            } else {
                const isFirst = container.querySelectorAll(".cart-item-row").length === 0;
                const tempDiv = document.createElement("div");
                tempDiv.innerHTML = buildCartRowHtml(
                    {
                        id: "3",
                        name: "Creatina Monohidratada 300g",
                        category: "Suplementos",
                        img: "/img/creatina.jpg",
                        price: 95.0,
                        qty: 1,
                        sub: 95.0
                    },
                    isFirst
                ).trim();
                const newRow = tempDiv.firstElementChild;
                if (newRow) {
                    container.appendChild(newRow);
                    bindRowEvents(newRow);
                }
            }
            recalculateCart();
            showGymToast("Creatina Monohidratada 300g agregada al carrito", "fa-cart-plus");
        });
    }

    const btnResetCart = document.getElementById("btnResetDemoCart");
    if (btnResetCart) {
        btnResetCart.addEventListener("click", () => {
            sessionStorage.removeItem("energyGymCartItems");
            sessionStorage.removeItem("energyGymCartTotal");
            sessionStorage.removeItem("energyGymBuyerInfo");
            window.location.reload();
        });
    }

    // 2. Sincronizar Paso 2 (Pago en Línea), Paso 3 (Exitosa, En Proceso, Rechazado, Cancelado), Detalle, Boleta y Mis Compras
    const activeItems = getStoredCartItems();
    const activeTotal = activeItems.reduce((acc, it) => acc + Number(it.price) * Number(it.qty), 0);
    const activeBase = activeTotal / 1.18;
    const activeIgv = activeTotal - activeBase;
    const activeTotalUnits = activeItems.reduce((acc, it) => acc + Number(it.qty), 0);

    // 2a. Paso 2: Pago en Línea (#checkoutItemsList)
    const checkoutList = document.getElementById("checkoutItemsList");
    if (checkoutList && activeItems.length > 0) {
        checkoutList.innerHTML = activeItems
            .map(
                (it) => `
                <div class="d-flex align-items-center justify-content-between gap-2 mb-2">
                    <div class="d-flex align-items-center gap-2">
                        <img src="${it.img}" alt="${it.name}" class="rounded border border-secondary bg-deep-dark p-1 obj-contain" style="width: 44px; height: 44px;">
                        <div>
                            <div class="text-light text-xs fw-bold">${it.qty} &times; ${it.name}</div>
                            <div class="text-muted text-2xs">${it.category}</div>
                        </div>
                    </div>
                    <span class="font-anton text-light text-xs">S/ ${(Number(it.price) * Number(it.qty)).toFixed(2)}</span>
                </div>`
            )
            .join("");

        const elBase = document.getElementById("checkoutSummaryBase");
        const elIgv = document.getElementById("checkoutSummaryIgv");
        const elTot = document.getElementById("checkoutSummaryTotal");
        const btnPay = document.getElementById("btnPayOnlineNow");
        if (elBase) elBase.textContent = `S/ ${activeBase.toFixed(2)}`;
        if (elIgv) elIgv.textContent = `S/ ${activeIgv.toFixed(2)}`;
        if (elTot) elTot.textContent = `S/ ${activeTotal.toFixed(2)}`;
        if (btnPay) {
            btnPay.innerHTML = `<span><i class="fa-solid fa-lock me-2"></i> PAGAR S/ ${activeTotal.toFixed(2)} AHORA</span><i class="fa-solid fa-arrow-right"></i>`;
        }
    }

    // 2b. Paso 3: Compra Exitosa / Pago en Proceso / Pago Rechazado / Pago Cancelado (#step3OrderItems)
    const step3OrderItems = document.getElementById("step3OrderItems");
    if (step3OrderItems && activeItems.length > 0) {
        const statusNote = step3OrderItems.getAttribute("data-status-note") || "";
        step3OrderItems.innerHTML = activeItems
            .map((it) => {
                const sub = (Number(it.price) * Number(it.qty)).toFixed(2);
                const subLabel = statusNote ? `${statusNote} &bull; S/ ${sub}` : `${it.category} &bull; S/ ${sub}`;
                return `
                <div class="d-flex align-items-center gap-3">
                    <img src="${it.img}" alt="${it.name}" class="rounded border border-secondary bg-deep-dark p-1 obj-contain" style="width: 48px; height: 48px;">
                    <div>
                        <div class="text-light text-xs fw-bold">${it.qty} &times; ${it.name}</div>
                        <div class="text-muted text-2xs">${subLabel}</div>
                    </div>
                </div>`;
            })
            .join("");

        const step3Total = document.getElementById("step3OrderTotal");
        if (step3Total) step3Total.textContent = `S/ ${activeTotal.toFixed(2)}`;
        const step3Tax = document.getElementById("step3OrderTaxBreakdown");
        if (step3Tax) step3Tax.textContent = `Base S/ ${activeBase.toFixed(2)} + IGV S/ ${activeIgv.toFixed(2)}`;
    }

    // 2c. Detalle del Pedido (#PED-00521 activo)
    const detalleActiveItems = document.getElementById("detallePedidoActiveItems");
    if (detalleActiveItems && activeItems.length > 0) {
        detalleActiveItems.innerHTML = activeItems
            .map((it, idx) => {
                const sub = (Number(it.price) * Number(it.qty)).toFixed(2);
                const borderCls = idx < activeItems.length - 1 ? "pb-3 mb-3 border-bottom border-secondary" : "";
                return `
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 ${borderCls}">
                    <div class="d-flex align-items-center gap-3">
                        <img src="${it.img}" alt="${it.name}" class="rounded border border-secondary bg-deep-dark p-1 obj-contain" style="width: 64px; height: 64px;">
                        <div>
                            <h6 class="font-anton text-light mb-1">${it.name}</h6>
                            <div class="text-muted text-2xs">${it.category} &bull; Cantidad: ${it.qty} &bull; Precio unitario: S/ ${Number(it.price).toFixed(2)}</div>
                        </div>
                    </div>
                    <div class="font-anton text-primary fs-5">S/ ${sub}</div>
                </div>`;
            })
            .join("");

        const countTitle = document.getElementById("detallePedidoCountTitle");
        if (countTitle) countTitle.textContent = `PRODUCTOS DEL PEDIDO (${activeTotalUnits})`;
        const detBase = document.getElementById("detallePedidoBase");
        if (detBase) detBase.textContent = `S/ ${activeBase.toFixed(2)}`;
        const detIgv = document.getElementById("detallePedidoIgv");
        if (detIgv) detIgv.textContent = `S/ ${activeIgv.toFixed(2)}`;
        const detTot = document.getElementById("detallePedidoTotal");
        if (detTot) detTot.textContent = `S/ ${activeTotal.toFixed(2)}`;
    }

    // 2d. Boleta de Venta (#PED-00521 activa)
    const boletaActiveTbody = document.getElementById("boletaActiveTbody");
    if (boletaActiveTbody && activeItems.length > 0) {
        boletaActiveTbody.innerHTML = activeItems
            .map((it) => {
                const sub = (Number(it.price) * Number(it.qty)).toFixed(2);
                return `
                <tr class="border-bottom border-secondary">
                    <td class="py-3 ps-0 text-light">${it.qty}</td>
                    <td class="py-3 text-light fw-bold">${it.name} (${it.category})</td>
                    <td class="py-3 text-end text-light">S/ ${Number(it.price).toFixed(2)}</td>
                    <td class="py-3 pe-0 text-end font-anton text-light">S/ ${sub}</td>
                </tr>`;
            })
            .join("");

        const bolBase = document.getElementById("boletaBase");
        if (bolBase) bolBase.textContent = `S/ ${activeBase.toFixed(2)}`;
        const bolIgv = document.getElementById("boletaIgv");
        if (bolIgv) bolIgv.textContent = `S/ ${activeIgv.toFixed(2)}`;
        const bolTot = document.getElementById("boletaTotal");
        if (bolTot) bolTot.textContent = `S/ ${activeTotal.toFixed(2)}`;
    }

    // 2e. Mis Compras (fila #PED-00521 activa)
    const misComprasThumbs = document.getElementById("misComprasActiveThumbs");
    const misComprasSummary = document.getElementById("misComprasActiveSummary");
    const misComprasTotal = document.getElementById("misComprasActiveTotal");
    if (misComprasSummary && activeItems.length > 0) {
        if (misComprasThumbs) {
            misComprasThumbs.innerHTML = activeItems
                .map(
                    (it) =>
                        `<img src="${it.img}" alt="${it.name}" class="rounded border border-secondary bg-deep-dark p-1 obj-contain" style="width: 56px; height: 56px;">`
                )
                .join("");
        }
        misComprasSummary.innerHTML = activeItems.map((it) => `${it.qty} &times; ${it.name}`).join(" &bull; ");
        if (misComprasTotal) misComprasTotal.textContent = `S/ ${activeTotal.toFixed(2)}`;
    }

    // 3. Vista previa interactiva de la tarjeta bancaria y persistencia de datos del comprador en /cliente/pago-en-linea
    const cardNumberInput = document.getElementById("cardNumberInput");
    const cardExpiryInput = document.getElementById("cardExpiryInput");
    const firstNameInput = document.getElementById("buyerFirstName");
    const lastNameInput = document.getElementById("buyerLastName");
    const docTypeInput = document.getElementById("buyerDocType");
    const docNumInput = document.getElementById("buyerDocNum");
    const emailInput = document.getElementById("buyerEmail");

    const saveBuyerInfo = () => {
        if (!firstNameInput) return;
        const digits = (cardNumberInput?.value || "").replace(/\D/g, "");
        const last4 = digits.length >= 4 ? digits.slice(-4) : "4242";
        const info = {
            fullName: `${firstNameInput.value || "Carlos"} ${lastNameInput?.value || "Mendoza Rojas"}`.trim(),
            docType: docTypeInput?.value || "DNI",
            docNum: docNumInput?.value || "72563123",
            email: emailInput?.value || "carlos.mendoza@ejemplo.com",
            last4
        };
        try {
            sessionStorage.setItem("energyGymBuyerInfo", JSON.stringify(info));
        } catch (e) {
            // ignore
        }
    };

    // Restaurar datos del comprador en Paso 3, Detalle del Pedido y Boleta si fueron editados
    try {
        const rawBuyer = sessionStorage.getItem("energyGymBuyerInfo");
        if (rawBuyer) {
            const b = JSON.parse(rawBuyer);
            const step3Buyer = document.getElementById("step3BuyerSummary");
            if (step3Buyer && b.fullName) {
                step3Buyer.innerHTML = `Comprador: <strong class="text-light">${b.fullName}</strong> &bull; ${b.docType} ${b.docNum}`;
            }
            const detName = document.getElementById("detallePedidoBuyerName");
            const detDoc = document.getElementById("detallePedidoBuyerDoc");
            if (detName && b.fullName) detName.textContent = b.fullName;
            if (detDoc && b.docNum) detDoc.textContent = `${b.docType} ${b.docNum} • ${b.email}`;

            const bolName = document.getElementById("boletaBuyerName");
            const bolDoc = document.getElementById("boletaBuyerDoc");
            const bolEmail = document.getElementById("boletaBuyerEmail");
            if (bolName && b.fullName) bolName.textContent = b.fullName;
            if (bolDoc && b.docNum) bolDoc.textContent = `${b.docType} ${b.docNum}`;
            if (bolEmail && b.email) bolEmail.textContent = b.email;
        }
    } catch (e) {
        // ignore
    }

    if (cardNumberInput) {
        cardNumberInput.addEventListener("input", function () {
            const digits = this.value.replace(/\D/g, "").slice(0, 16);
            const formatted = digits.replace(/(\d{4})(?=\d)/g, "$1 ");
            this.value = formatted;

            const visualNum = document.getElementById("visualCardNumber");
            if (visualNum) {
                visualNum.textContent = formatted || "4557 •••• •••• 4242";
            }
            const brandIcon = document.getElementById("visualCardBrandIcon");
            if (brandIcon) {
                if (digits.startsWith("5")) {
                    brandIcon.className = "fa-brands fa-cc-mastercard fs-3 text-warning";
                } else if (digits.startsWith("3")) {
                    brandIcon.className = "fa-brands fa-cc-amex fs-3 text-info";
                } else {
                    brandIcon.className = "fa-brands fa-cc-visa fs-3 text-light";
                }
            }
            saveBuyerInfo();
        });
    }

    if (cardExpiryInput) {
        cardExpiryInput.addEventListener("input", function () {
            const visualExp = document.getElementById("visualCardExpiry");
            if (visualExp) visualExp.textContent = this.value || "08 / 28";
        });
    }

    const updateHolderPreview = () => {
        const visualHolder = document.getElementById("visualCardHolder");
        if (visualHolder) {
            const full = `${firstNameInput?.value || ""} ${lastNameInput?.value || ""}`.trim().toUpperCase();
            visualHolder.textContent = full || "CARLOS MENDOZA ROJAS";
        }
        saveBuyerInfo();
    };
    if (firstNameInput) firstNameInput.addEventListener("input", updateHolderPreview);
    if (lastNameInput) lastNameInput.addEventListener("input", updateHolderPreview);
    if (docTypeInput) docTypeInput.addEventListener("change", saveBuyerInfo);
    if (docNumInput) docNumInput.addEventListener("input", saveBuyerInfo);
    if (emailInput) emailInput.addEventListener("input", saveBuyerInfo);

    document.querySelectorAll(".btn-download-boleta").forEach((btn) => {
        btn.addEventListener("click", function () {
            showGymToast("Generando representación impresa de la Boleta Electrónica...", "fa-file-invoice");
        });
    });
}
