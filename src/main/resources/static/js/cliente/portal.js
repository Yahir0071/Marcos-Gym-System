import { createDomElement } from "../core/modal.js";
import { showGymToast } from "../core/toast.js";
import { initPasarelaSimulada } from "./pago.js";

export { initWorkoutTracker } from "./rutina.js";

const cartKey = "energyGymCartItems";
const soles = (amount) => `S/ ${Number(amount).toFixed(2)}`;

function loadCart() {
    try {
        const stored = JSON.parse(sessionStorage.getItem(cartKey) || "[]");
        if (!Array.isArray(stored) || sessionStorage.getItem("energyGymCartPaid") === "true") return [];
        return stored.map(item => itemFromData(item, item?.qty)).filter(Boolean);
    } catch {
        return [];
    }
}

function itemFromData(data, quantity) {
    const id = Number(data?.id);
    const price = Number(data?.price);
    const qty = Number(quantity);
    if (!Number.isSafeInteger(id) || id < 1 || !Number.isFinite(price) || price < 0 ||
        !Number.isSafeInteger(qty) || qty < 1 || typeof data.name !== "string") return null;
    return {
        id,
        name: data.name,
        category: String(data.category || "Otros"),
        img: String(data.img || ""),
        price,
        qty
    };
}

function productPhoto(item, width) {
    const box = createDomElement("div", "d-flex align-items-center justify-content-center rounded border border-secondary bg-deep-dark p-1 flex-shrink-0");
    box.style.width = width;
    box.style.height = width;
    if (!item.img) return box;
    try {
        const url = new URL(item.img, window.location.origin);
        if (!["http:", "https:"].includes(url.protocol)) return box;
        const img = createDomElement("img", "img-fluid h-100 obj-contain");
        img.src = url.href;
        img.alt = item.name;
        box.append(img);
    } catch {
        // El catálogo puede contener productos sin imagen válida.
    }
    return box;
}

function cartRow(item, render) {
    const row = createDomElement("article", "cart-item-row d-flex align-items-center justify-content-between flex-wrap gap-3 p-3 border-bottom border-secondary");
    const product = createDomElement("div", "d-flex align-items-center gap-3");
    const details = createDomElement("div");
    details.append(
        createDomElement("p", "font-anton text-primary text-xs mb-1", item.category),
        createDomElement("h2", "font-anton text-light fs-5 mb-1", item.name),
        createDomElement("p", "text-muted text-xs mb-0", `Precio unitario ${soles(item.price)}`)
    );
    product.append(productPhoto(item, "64px"), details);

    const quantity = createDomElement("div", "d-flex align-items-center gap-2");
    const minus = createDomElement("button", "btn btn-outline px-3", "−");
    const plus = createDomElement("button", "btn btn-outline px-3", "+");
    const remove = createDomElement("button", "btn btn-link text-danger px-0", "Eliminar");
    for (const button of [minus, plus, remove]) button.type = "button";
    minus.setAttribute("aria-label", `Disminuir cantidad de ${item.name}`);
    plus.setAttribute("aria-label", `Aumentar cantidad de ${item.name}`);
    remove.setAttribute("aria-label", `Eliminar ${item.name}`);
    minus.disabled = item.qty <= 1;
    plus.disabled = item.qty >= Number.MAX_SAFE_INTEGER;
    minus.onclick = () => render(item.id, item.qty - 1, 0);
    plus.onclick = () => render(item.id, item.qty + 1, 1);
    remove.onclick = () => render(item.id, 0, 2);
    quantity.append(minus, createDomElement("span", "text-light", item.qty), plus);

    const actions = createDomElement("div", "text-end ms-auto");
    actions.append(createDomElement("strong", "text-primary font-anton", soles(item.qty * item.price)), remove);
    row.append(product, quantity, actions);
    return row;
}

function checkoutItems(cart) {
    const reservation = document.getElementById("reservationCheckout");
    if (reservation) {
        const item = itemFromData({ ...reservation.dataset, id: reservation.dataset.productId }, reservation.dataset.qty);
        return item ? [item] : [];
    }
    const plan = document.getElementById("planCheckout");
    if (plan) return [{ name: plan.dataset.name, category: "Membresía", img: "", price: Number(plan.dataset.price), qty: 1 }];
    return cart;
}

function renderCheckout(items) {
    const list = document.getElementById("checkoutItemsList");
    if (!list) return;
    if (!items.length) {
        const message = createDomElement("p", "text-muted mb-2", "Tu carrito está vacío.");
        const link = createDomElement("a", "btn btn-outline", "Volver al catálogo");
        link.href = "/cliente/productos";
        list.replaceChildren(message, link);
        return;
    }
    const lines = items.map(item => {
        const line = createDomElement("div", "d-flex align-items-center justify-content-between gap-3 mb-2");
        const info = createDomElement("div", "d-flex align-items-center gap-2");
        const details = createDomElement("div");
        details.append(createDomElement("div", "text-light", `${item.qty} × ${item.name}`),
            createDomElement("span", "text-muted text-xs", item.category));
        if (item.img) info.append(productPhoto(item, "44px"));
        info.append(details);
        line.append(info, createDomElement("strong", "text-light text-nowrap", soles(item.qty * item.price)));
        return line;
    });
    list.replaceChildren(...lines);
    document.getElementById("checkoutSummaryTotal").textContent = soles(items.reduce((total, item) => total + item.price * item.qty, 0));
    const totalRow = document.getElementById("checkoutTotalRow");
    totalRow.classList.remove("d-none");
    totalRow.classList.add("d-flex");
    document.getElementById("checkoutActions").hidden = false;
}

export function initCartAndCompras() {
    let cart = loadCart();
    const cartList = document.getElementById("cartItemsContainer");
    const cartBadge = document.getElementById("headerCartBadge");

    const saveCart = () => {
        if (cartBadge) cartBadge.textContent = String(cart.reduce((sum, item) => sum + item.qty, 0));
        try {
            sessionStorage.setItem(cartKey, JSON.stringify(cart));
            sessionStorage.removeItem("energyGymCartPaid");
            sessionStorage.removeItem("energyGymCartTotal");
            sessionStorage.removeItem("energyGymBuyerInfo");
        } catch {
            // Mantener operativa la vista actual aunque el navegador bloquee el almacenamiento.
        }
    };

    const renderCart = (focusId, focusIndex) => {
        if (!cartList) return;
        const changeQuantity = (id, qty, index) => {
            if (qty === 0) cart = cart.filter(item => item.id !== id);
            else cart.find(item => item.id === id).qty = qty;
            saveCart();
            renderCart(id, index);
        };
        cartList.replaceChildren(...cart.map(item => cartRow(item, changeQuantity)));
        const empty = document.getElementById("cartEmpty");
        const actions = document.getElementById("cartActions");
        const headerCounter = document.getElementById("cartHeaderCounter");
        empty.hidden = cart.length > 0;
        actions.hidden = cart.length === 0;
        headerCounter.textContent = `${cart.reduce((sum, item) => sum + item.qty, 0)} unidades`;
        document.getElementById("cartSummaryTotal").textContent = soles(cart.reduce((sum, item) => sum + item.price * item.qty, 0));
        if (focusId) {
            const rowIndex = cart.findIndex(item => item.id === focusId);
            const buttons = cartList.children[rowIndex]?.querySelectorAll("button");
            const focus = buttons?.[focusIndex];
            if (focus && !focus.disabled) focus.focus();
            else if (buttons) Array.from(buttons).find(button => !button.disabled)?.focus();
            else (cartList.querySelector("button") || empty.querySelector("a")).focus();
        }
    };

    saveCart();
    renderCart();
    const clearCartButton = document.getElementById("btnClearCart");
    clearCartButton?.addEventListener("click", () => {
        cart = [];
        saveCart();
        renderCart();
        document.querySelector("#cartEmpty a")?.focus();
    });

    document.querySelectorAll(".btn-quick-add-cart, #btnAddToCartDetail").forEach(btn => {
        btn.addEventListener("click", event => {
            event.preventDefault();
            const detail = btn.id === "btnAddToCartDetail";
            const qty = detail ? Number(document.getElementById("detailQtyInput")?.value) : 1;
            const item = itemFromData(btn.dataset, qty);
            if (!item) {
                showGymToast("El producto no está disponible.", "fa-circle-exclamation", "error");
                return;
            }
            const existing = cart.find(product => product.id === item.id);
            const totalQuantity = (existing?.qty || 0) + item.qty;
            if (!Number.isSafeInteger(totalQuantity)) {
                showGymToast("La cantidad supera el límite permitido.", "fa-circle-exclamation", "error");
                return;
            }
            if (existing) Object.assign(existing, item, { qty: totalQuantity });
            else cart.push(item);
            saveCart();
            if (detail) window.location.href = btn.href;
            else showGymToast(`${item.name} agregado al carrito`, "fa-cart-plus");
        });
    });

    renderCheckout(checkoutItems(cart));
    initPasarelaSimulada({
        getStoredCartItems: () => cart,
        onCartOrdered: () => {
            cart = [];
            saveCart();
        }
    });
}
