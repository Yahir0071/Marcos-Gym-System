/**
 * cliente/pago.js - Pasarela de pago SIMULADA (proyecto académico, sin cobros reales).
 * Solo navega a "Compra exitosa" cuando el servidor confirma que el pedido se guardó.
 */
import { postJson } from "../core/api-client.js";
import { showGymToast } from "../core/toast.js";

const nuevaReferencia = () =>
    (window.crypto && crypto.randomUUID)
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(16).slice(2)}`;

export function initPasarelaSimulada({ getStoredCartItems, saveBuyerInfo }) {
    let referenciaPendiente = null;   // se reutiliza SOLO si no hubo respuesta del servidor (error de red)
    let procesando = false;

    const bloquear = (btn, valor) => {
        procesando = valor;
        btn.classList.toggle("disabled", valor);
        btn.setAttribute("aria-disabled", String(valor));
    };

    // ---- 1) Botones de escenario: PAGADO / EN_PROCESO / RECHAZADO / CANCELADO
    document.querySelectorAll(".btn-process-gateway").forEach((btn) => {
        btn.addEventListener("click", async (e) => {
            e.preventDefault();
            if (procesando) return;

            const targetHref = btn.getAttribute("href") || "/cliente/compra-exitosa";
            const scenario = btn.getAttribute("data-gateway-scenario") || "PAGADO";

            // Solo se envía el id y la cantidad: el precio y el nombre los decide el servidor
            const items = getStoredCartItems().map((it) => ({ id: Number(it.id), qty: Number(it.qty) }));
            if (items.length === 0) {
                showGymToast("Tu carrito está vacío.", "fa-cart-shopping");
                return;
            }

            const digits = (document.getElementById("cardNumberInput")?.value || "").replace(/\D/g, "");
            const payload = {
                referencia: referenciaPendiente ?? nuevaReferencia(),
                estado: scenario,
                cardBrand: digits.startsWith("5") ? "MASTERCARD" : digits.startsWith("3") ? "AMEX" : "VISA",
                cardLast4: digits.length >= 4 ? digits.slice(-4) : "4242",
                items
            };
            const reservationCheckout = document.getElementById("reservationCheckout");
            if (reservationCheckout?.dataset.reservationId) {
                payload.reservaId = Number(reservationCheckout.dataset.reservationId);
            }

            saveBuyerInfo();
            bloquear(btn, true);
            try {
                const resp = await postJson("/cliente/api/compras", payload);
                referenciaPendiente = null;                     // hubo respuesta: la referencia ya cumplió su función
                const data = await resp.json().catch(() => ({}));

                if (resp.status === 401) {
                    window.location.href = "/login";
                    return;
                }
                if (!resp.ok || !data.success || !data.codigo) {
                    const detalle = data.errores ? Object.values(data.errores)[0] : null;
                    showGymToast(
                        detalle || data.mensaje || `No se pudo procesar el pago (${resp.status}). Tu carrito sigue intacto.`,
                        "fa-triangle-exclamation"
                    );
                    bloquear(btn, false);
                    return;                                     // NO navega y NO borra el carrito
                }

                if (scenario === "PAGADO" || scenario === "EN_PROCESO") {
                    sessionStorage.setItem("energyGymCartPaid", "true");
                }
                window.location.href = `${targetHref}?codigo=${encodeURIComponent(data.codigo)}`;
            } catch (err) {
                referenciaPendiente = payload.referencia;       // reintento seguro: el servidor detecta el duplicado
                showGymToast("Error de red. Puedes reintentar sin duplicar la operación.", "fa-wifi");
                bloquear(btn, false);
            }
        });
    });

    // ---- 2) Aprobar o cancelar una operación pendiente
    const resolverPendiente = (selector, endpoint, mensajeError) => {
        const btn = document.querySelector(selector);
        if (!btn) return;
        btn.addEventListener("click", async (e) => {
            e.preventDefault();
            if (procesando) return;

            const codigo = btn.getAttribute("data-codigo");
            const targetHref = btn.getAttribute("href");
            bloquear(btn, true);
            try {
                const resp = await postJson(`${endpoint}?codigo=${encodeURIComponent(codigo)}`, {});
                const data = await resp.json().catch(() => ({}));
                if (!resp.ok || !data.success) {
                    showGymToast(data.mensaje || mensajeError, "fa-triangle-exclamation");
                    bloquear(btn, false);
                    return;
                }
                window.location.href = targetHref;
            } catch (err) {
                showGymToast("Error de red. Intenta nuevamente.", "fa-wifi");
                bloquear(btn, false);
            }
        });
    };
    resolverPendiente("#btnApprovePendingOrder", "/cliente/api/compras/aprobar", "No se pudo aprobar la operación.");
    resolverPendiente("#btnCancelPendingOrder", "/cliente/api/compras/cancelar", "No se pudo cancelar la operación.");
}