/**
 * cliente/congelamiento.js - Componente independiente de Solicitud Persistente de Congelamiento (M08)
 * y Voucher Seguro de Reservas (C04).
 */

import { getJson, postJson } from "../core/api-client.js";
import { openSafeGymModal, closeGymModal, createDomElement, createIcon } from "../core/modal.js";
import { showGymToast } from "../core/toast.js";

function renderCongelamientoList(container, solicitudes) {
    if (!container) return;
    container.replaceChildren();

    if (!Array.isArray(solicitudes) || solicitudes.length === 0) {
        container.appendChild(
            createDomElement("p", "text-muted text-xs mb-0", "No tienes solicitudes de congelamiento registradas.")
        );
        return;
    }

    const list = createDomElement("div", "d-flex flex-column gap-2");
    solicitudes.forEach((sol) => {
        const card = createDomElement("div", "p-3 rounded bg-elevated border border-secondary");
        const top = createDomElement("div", "d-flex justify-content-between align-items-center flex-wrap gap-2 mb-1");
        top.append(
            createDomElement("strong", "font-anton text-light text-xs", `${sol.dias} DÍAS · DESDE ${sol.fechaInicioSolicitada}`),
            createDomElement(
                "span",
                `badge ${sol.estado === "APROBADA" ? "bg-success text-dark" : (sol.estado === "RECHAZADA" ? "bg-danger" : "bg-warning text-dark")} font-anton text-2xs`,
                sol.estado || "PENDIENTE"
            )
        );
        const motivo = createDomElement("p", "text-muted text-xs mb-1", sol.motivo || "");
        const fechaReg = createDomElement("small", "text-muted text-2xs", `Registrada el ${sol.fechaSolicitud || "—"}`);
        card.append(top, motivo, fechaReg);
        list.appendChild(card);
    });

    container.appendChild(list);
}

export async function initCongelamiento() {
    const triggerBtn = document.getElementById("btnSolicitarCongelamiento");
    const historyContainer = document.getElementById("congelamientoHistoryList");
    if (!triggerBtn && !historyContainer) return;

    const loadSolicitudes = async () => {
        if (!historyContainer) return;
        try {
            const resp = await getJson("/cliente/api/congelamiento");
            if (!resp.ok || resp.redirected) {
                historyContainer.replaceChildren(
                    createDomElement("p", "text-muted text-xs mb-0", "No se pudo consultar el historial de congelamiento.")
                );
                return;
            }
            const data = await resp.json();
            renderCongelamientoList(historyContainer, data.solicitudes || []);
        } catch {
            historyContainer.replaceChildren(
                createDomElement("p", "text-muted text-xs mb-0", "Error al consultar solicitudes de congelamiento.")
            );
        }
    };

    if (historyContainer) {
        loadSolicitudes();
    }

    if (triggerBtn) {
        triggerBtn.addEventListener("click", () => {
            const form = createDomElement("form");
            form.id = "formSolicitudCongelamiento";

            const intro = createDomElement(
                "p",
                "text-muted text-xs mb-3",
                "Registra una solicitud formal para pausar tu membresía activa por viaje o descanso médico (entre 7 y 15 días). Tu solicitud quedará guardada para revisión en recepción."
            );

            const feedbackBox = createDomElement("div");
            feedbackBox.id = "congelamientoModalFeedback";

            const diasGroup = createDomElement("div", "mb-3");
            const diasLabel = createDomElement("label", "form-label", "CANTIDAD DE DÍAS A CONGELAR *");
            diasLabel.htmlFor = "congelamientoDias";
            const diasSelect = createDomElement("select", "form-select");
            diasSelect.id = "congelamientoDias";
            diasSelect.name = "dias";
            diasSelect.required = true;
            [7, 10, 15].forEach((d) => {
                const opt = createDomElement("option", "", `${d} días`);
                opt.value = String(d);
                diasSelect.appendChild(opt);
            });
            diasGroup.append(diasLabel, diasSelect);

            const fechaGroup = createDomElement("div", "mb-3");
            const fechaLabel = createDomElement("label", "form-label", "FECHA DE INICIO SOLICITADA *");
            fechaLabel.htmlFor = "congelamientoFechaInicio";
            const fechaInput = createDomElement("input", "form-control");
            fechaInput.type = "date";
            fechaInput.id = "congelamientoFechaInicio";
            fechaInput.name = "fechaInicioSolicitada";
            fechaInput.min = triggerBtn.dataset.fechaMinima;
            fechaInput.value = fechaInput.min;
            fechaInput.required = true;
            fechaGroup.append(fechaLabel, fechaInput);

            const motivoGroup = createDomElement("div", "mb-3");
            const motivoLabel = createDomElement("label", "form-label", "MOTIVO DE LA SOLICITUD *");
            motivoLabel.htmlFor = "congelamientoMotivo";
            const motivoTextarea = createDomElement("textarea", "form-control");
            motivoTextarea.id = "congelamientoMotivo";
            motivoTextarea.name = "motivo";
            motivoTextarea.rows = 3;
            motivoTextarea.minLength = 5;
            motivoTextarea.maxLength = 300;
            motivoTextarea.placeholder = "Indica brevemente el motivo (viaje, descanso médico, etc.)...";
            motivoTextarea.required = true;
            motivoGroup.append(motivoLabel, motivoTextarea);

            const submitBtn = createDomElement("button", "btn btn-primary w-100 py-2");
            submitBtn.type = "submit";
            submitBtn.id = "btnConfirmarCongelamiento";
            submitBtn.append(createIcon("fa-regular fa-snowflake me-1"), document.createTextNode("ENVIAR SOLICITUD"));

            form.append(intro, feedbackBox, diasGroup, fechaGroup, motivoGroup, submitBtn);

            form.addEventListener("submit", async (ev) => {
                ev.preventDefault();
                feedbackBox.replaceChildren();

                const motivo = motivoTextarea.value.trim();
                if (motivo.length < 5) {
                    const err = createDomElement("div", "alert alert-danger bg-dark text-danger border border-danger py-2 px-3 text-xs mb-3", "El motivo debe tener al menos 5 caracteres.");
                    err.setAttribute("role", "alert");
                    feedbackBox.appendChild(err);
                    return;
                }

                submitBtn.disabled = true;
                try {
                    const resp = await postJson("/cliente/api/congelamiento", {
                        dias: parseInt(diasSelect.value, 10),
                        fechaInicioSolicitada: fechaInput.value,
                        motivo
                    });
                    const data = await resp.json().catch(() => ({}));
                    if (!resp.ok || data.success === false) {
                        const msg = data.mensaje || "No se pudo registrar la solicitud de congelamiento.";
                        const err = createDomElement("div", "alert alert-danger bg-dark text-danger border border-danger py-2 px-3 text-xs mb-3", msg);
                        err.setAttribute("role", "alert");
                        feedbackBox.appendChild(err);
                        showGymToast(msg, "fa-triangle-exclamation", "error");
                        return;
                    }
                    closeGymModal();
                    showGymToast(data.mensaje || "Solicitud de congelamiento registrada correctamente.", "fa-snowflake");
                    await loadSolicitudes();
                } catch {
                    const msg = "Error de conexión al registrar la solicitud de congelamiento.";
                    const err = createDomElement("div", "alert alert-danger bg-dark text-danger border border-danger py-2 px-3 text-xs mb-3", msg);
                    err.setAttribute("role", "alert");
                    feedbackBox.appendChild(err);
                    showGymToast(msg, "fa-triangle-exclamation", "error");
                } finally {
                    submitBtn.disabled = false;
                }
            });

            openSafeGymModal("SOLICITAR CONGELAMIENTO DE MEMBRESÍA", form, "fa-regular fa-snowflake text-info me-2");
        });
    }
}

export function initSafeReservaVoucher() {
    document.querySelectorAll(".btn-view-reserva-voucher").forEach((btn) => {
        if (btn.dataset.bound) return;
        btn.dataset.bound = "true";
        btn.addEventListener("click", function () {
            const code = this.getAttribute("data-code") || "#RES-00001";
            const prod = this.getAttribute("data-product") || "Producto";
            const qty = this.getAttribute("data-qty") || "1";
            const status = this.getAttribute("data-status") || "PENDIENTE";
            const socio = this.getAttribute("data-socio") || "Socio Energy Gym";

            const body = createDomElement("div", "p-2");
            const card = createDomElement("div", "p-3 rounded bg-elevated border border-secondary mb-3");

            const addRow = (lbl, val, cls) => {
                const r = createDomElement("div", "d-flex justify-content-between gap-2 mb-2");
                r.append(createDomElement("span", "text-muted text-xs", lbl), createDomElement("strong", cls, val));
                card.appendChild(r);
            };

            addRow("CÓDIGO:", code, "font-anton text-primary");
            addRow("SOCIO:", socio, "text-light text-xs");
            addRow("PRODUCTO:", `${prod} (x${qty})`, "text-light text-xs");

            const statusRow = createDomElement("div", "d-flex justify-content-between align-items-center");
            statusRow.append(
                createDomElement("span", "text-muted text-xs", "ESTADO ACTUAL:"),
                createDomElement("span", "badge bg-primary text-dark font-anton", status)
            );
            card.appendChild(statusRow);

            const note = createDomElement(
                "p",
                "text-muted text-2xs mb-3",
                "Presenta esta constancia en recepción de la sede de Las Américas, Ate, para gestionar tu reserva."
            );
            const closeBtn = createDomElement("button", "btn btn-primary w-100", "CERRAR CONSTANCIA");
            closeBtn.type = "button";
            closeBtn.setAttribute("data-bs-dismiss", "modal");

            body.append(card, note, closeBtn);
            openSafeGymModal(`CONSTANCIA DE RESERVA ${code}`, body, "fa-solid fa-receipt text-primary me-2");
        });
    });
}
