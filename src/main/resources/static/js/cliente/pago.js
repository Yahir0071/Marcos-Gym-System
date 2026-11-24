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

export function initPasarelaSimulada({ getStoredCartItems, onCartOrdered }) {
    let referenciaPendiente = null;   // se reutiliza SOLO si no hubo respuesta del servidor (error de red)
    let procesando = false;
    const checkoutError = document.getElementById("checkoutError");

    const informarError = (mensaje) => {
        if (checkoutError) checkoutError.textContent = mensaje;
        else showGymToast(mensaje, "fa-circle-exclamation", "error");
    };

    const bloquear = (btn, valor) => {
        procesando = valor;
        if (btn instanceof HTMLButtonElement) btn.disabled = valor;
        btn.classList.toggle("disabled", valor);
        btn.setAttribute("aria-disabled", String(valor));
    };

    // ---- 1) Botones de escenario: PAGADO / EN_PROCESO / RECHAZADO / CANCELADO
    document.querySelectorAll(".btn-process-gateway").forEach((btn) => {
        btn.addEventListener("click", async (e) => {
            e.preventDefault();
            if (procesando) return;

            const reservationCheckout = document.getElementById("reservationCheckout");
            const planCheckout = document.getElementById("planCheckout");
            const payload = {
                referencia: referenciaPendiente ?? nuevaReferencia(),
                estado: btn.dataset.gatewayScenario || "PAGADO"
            };
            if (planCheckout) {
                payload.tipoConcepto = "MEMBRESIA";
                payload.planId = Number(planCheckout.dataset.planId);
                payload.periodo = planCheckout.dataset.periodo;
            } else if (reservationCheckout) {
                payload.reservaId = Number(reservationCheckout.dataset.reservationId);
            } else {
                payload.items = getStoredCartItems().map(({ id, qty }) => ({ id, qty }));
                if (!payload.items.length) {
                    informarError("Tu carrito está vacío.");
                    return;
                }
            }

            if (checkoutError) checkoutError.textContent = "";
            bloquear(btn, true);
            try {
                const resp = await postJson("/cliente/api/compras", payload);
                const data = await resp.json().catch(() => ({}));
                referenciaPendiente = (resp.status >= 500 || (resp.ok && !data.codigo)) ? payload.referencia : null;

                if (resp.status === 401 || resp.redirected) {
                    window.location.href = "/login";
                    return;
                }
                if (!resp.ok || !data.success || !data.codigo) {
                    const detalle = data.errores ? Object.values(data.errores)[0] : null;
                    informarError(detalle || data.mensaje || "No se pudo procesar la operación. Puedes reintentarlo.");
                    bloquear(btn, false);
                    return;                                     // NO navega y NO borra el carrito
                }

                const rutas = {PAGADO: "/cliente/compra-exitosa", EN_PROCESO: "/cliente/pago-en-proceso",
                    RECHAZADO: "/cliente/pago-rechazado", CANCELADO: "/cliente/pago-cancelado"};
                if (!Object.hasOwn(rutas, data.estadoPago)) {
                    referenciaPendiente = payload.referencia;
                    informarError("No se pudo confirmar el resultado. Puedes reintentarlo.");
                    bloquear(btn, false);
                    return;
                }
                if (!reservationCheckout && !planCheckout &&
                    (data.estadoPago === "PAGADO" || data.estadoPago === "EN_PROCESO")) {
                    onCartOrdered();
                }
                window.location.href = `${rutas[data.estadoPago]}?codigo=${encodeURIComponent(data.codigo)}`;
            } catch (err) {
                referenciaPendiente = payload.referencia;       // reintento seguro: el servidor detecta el duplicado
                informarError("Error de conexión. Puedes reintentar sin duplicar la operación.");
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
