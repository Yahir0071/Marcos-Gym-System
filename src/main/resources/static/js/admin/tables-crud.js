/**
 * admin/tables-crud.js - Energy Gym Administration Engine (DOM Seguro - C04 / C08 / M07 / M15)
 * Responsabilidad: Filtrado de tablas, gráfico con datos reales, pestañas, edición persistente
 * de socios contra el contrato REST de Esaul y comprobantes académicos sin innerHTML.
 */

import { putJson, patchRequest } from "../core/api-client.js";
import { openSafeGymModal, closeGymModal, createDomElement, createIcon } from "../core/modal.js";
import { showGymToast } from "../core/toast.js";

export function initTableFilters() {
    const applyTableFilters = () => {
        const table = document.querySelector(".filterable-table");
        if (!table) return;
        const searchInput = document.querySelector(".table-search-input");
        const query = (searchInput ? searchInput.value : "").toLowerCase().trim();
        const selects = Array.from(document.querySelectorAll(".table-filter-select"));

        table.querySelectorAll("tbody tr").forEach((row) => {
            if (row.children.length <= 1) return;
            const text = row.textContent.toLowerCase();
            let matchesSelects = true;
            selects.forEach((sel) => {
                const val = sel.value.toUpperCase().trim();
                const colIdx = parseInt(sel.getAttribute("data-filter-col"), 10);
                if (val && !isNaN(colIdx) && row.children[colIdx]) {
                    const cellText = row.children[colIdx].textContent.toUpperCase().trim();
                    if (!cellText.includes(val)) matchesSelects = false;
                }
            });
            row.style.display = (!query || text.includes(query)) && matchesSelects ? "" : "none";
        });
    };

    document.querySelectorAll(".table-search-input").forEach((inp) => inp.addEventListener("input", applyTableFilters));
    document.querySelectorAll(".table-filter-select").forEach((sel) => sel.addEventListener("change", applyTableFilters));
}

export function initDashboardChart() {
    const chartContainer = document.getElementById("dashboardBarChart");
    const chartPeriodBtns = document.querySelectorAll(".chart-period-btn");
    if (!chartContainer || chartPeriodBtns.length === 0) return;

    const parseDataset = (attrName) => {
        try {
            const raw = chartContainer.getAttribute(attrName);
            const parsed = raw ? JSON.parse(raw) : [];
            return Array.isArray(parsed) ? parsed : [];
        } catch {
            return [];
        }
    };

    const datasets = {
        semana: parseDataset("data-semana"),
        mes: parseDataset("data-mes")
    };

    const renderBars = (periodKey) => {
        const data = datasets[periodKey] || [];
        const hasAny = data.some((d) => Number(d.valor) > 0);
        const emptyNotice = document.getElementById("dashboardChartEmptyNotice");
        if (emptyNotice) {
            emptyNotice.classList.toggle("d-none", hasAny);
        }

        const vals = chartContainer.querySelectorAll(".bar-val");
        const fills = chartContainer.querySelectorAll(".bar-fill");
        data.forEach((item, idx) => {
            const valor = Number(item.valor) || 0;
            const altura = Number(item.altura) || 0;
            if (vals[idx]) {
                vals[idx].textContent = String(valor);
                vals[idx].className = valor > 0 ? "text-xs fw-bold text-primary mb-1 bar-val" : "text-xs fw-bold text-muted mb-1 bar-val";
            }
            if (fills[idx]) {
                fills[idx].style.height = `${valor > 0 ? altura : 4}%`;
                fills[idx].classList.toggle("bg-primary", valor > 0);
                fills[idx].style.backgroundColor = valor > 0 ? "" : "#2c3548";
            }
        });
    };

    chartPeriodBtns.forEach((btn) => {
        btn.addEventListener("click", function () {
            chartPeriodBtns.forEach((b) => {
                b.classList.remove("btn-primary", "active");
                b.classList.add("btn-outline");
                b.setAttribute("aria-pressed", "false");
            });
            this.classList.remove("btn-outline");
            this.classList.add("btn-primary", "active");
            this.setAttribute("aria-pressed", "true");

            const period = this.getAttribute("data-period") || "semana";
            renderBars(period);
        });
    });

    renderBars("semana");
}

export function initAdminTabs() {
    document.querySelectorAll(".tab-btn[data-tab-target]").forEach((btn) => {
        btn.addEventListener("click", function () {
            document.querySelectorAll(".tab-btn[data-tab-target]").forEach((b) => {
                b.classList.remove("active");
                b.setAttribute("aria-selected", "false");
            });
            this.classList.add("active");
            this.setAttribute("aria-selected", "true");
            const target = this.getAttribute("data-tab-target");
            document.querySelectorAll(".admin-tab-pane").forEach((pane) => {
                pane.style.display = target === "all" || pane.id === target ? "" : "none";
            });
        });
    });
}

function buildLabeledField(id, labelText, inputEl) {
    const group = createDomElement("div", "mb-3");
    const label = createDomElement("label", "form-label", labelText);
    label.htmlFor = id;
    inputEl.id = id;
    group.append(label, inputEl);
    return group;
}

function buildAlertBanner(text, variant = "danger") {
    const alert = createDomElement(
        "div",
        `alert alert-${variant} bg-dark text-${variant} border border-${variant} py-2 px-3 text-xs mb-3`,
        ""
    );
    alert.append(createIcon("fa-solid fa-triangle-exclamation me-2"), document.createTextNode(text));
    return alert;
}

/**
 * Envía actualización de socio al contrato pactado de Esaul:
 *   PUT /api/admin/socios/{id}  -> { nombres, apellidos, telefono, activo }
 *   PATCH /api/admin/socios/{id}/estado?activo=true|false
 */
async function requestSocioUpdate(id, payload) {
    const resp = await putJson(`/api/admin/socios/${encodeURIComponent(id)}`, payload);
    const data = await resp.json().catch(() => ({}));
    return { ok: resp.ok && !resp.redirected, status: resp.status, data };
}

async function requestSocioToggleEstado(id, activo) {
    const url = `/api/admin/socios/${encodeURIComponent(id)}/estado?activo=${encodeURIComponent(String(activo))}`;
    const resp = await patchRequest(url);
    const data = await resp.json().catch(() => ({}));
    return { ok: resp.ok && !resp.redirected, status: resp.status, data };
}

function renderSocioStatusCell(statusCell, activo) {
    if (!statusCell) return;
    statusCell.replaceChildren();
    const badge = createDomElement("span", activo ? "" : "status-inactive", activo ? "ACTIVO" : "INACTIVO");
    statusCell.appendChild(badge);
}

export function initSocioEditModal() {
    document.querySelectorAll(".btn-edit-socio").forEach((btn) => {
        if (btn.dataset.bound) return;
        btn.dataset.bound = "true";

        btn.addEventListener("click", function (e) {
            e.preventDefault();
            const row = this.closest("tr");
            const socioId = this.dataset.id;
            if (!socioId || !row) return;

            const currentNombres = this.dataset.nombres || "";
            const currentApellidos = this.dataset.apellidos || "";
            const currentDni = this.dataset.dni || "";
            const currentTelefono = this.dataset.telefono || "";
            const currentEmail = this.dataset.email || "";
            const currentActivo = this.dataset.activo !== "false";

            const form = createDomElement("form");
            form.id = "adminEditSocioForm";
            form.noValidate = false;

            const feedbackBox = createDomElement("div");
            feedbackBox.id = "socioEditFeedback";
            feedbackBox.setAttribute("aria-live", "polite");
            form.appendChild(feedbackBox);

            // Resumen de solo lectura (DNI y usuario con textContent seguro)
            const metaCard = createDomElement("div", "p-3 rounded bg-elevated border border-secondary mb-3 text-xs");
            const metaDni = createDomElement("div", "d-flex justify-content-between mb-1");
            metaDni.append(
                createDomElement("span", "text-muted", "DOCUMENTO (DNI):"),
                createDomElement("strong", "text-light", currentDni || "—")
            );
            const metaEmail = createDomElement("div", "d-flex justify-content-between");
            metaEmail.append(
                createDomElement("span", "text-muted", "USUARIO:"),
                createDomElement("span", "text-primary fw-bold", currentEmail || "Sin usuario")
            );
            metaCard.append(metaDni, metaEmail);
            form.appendChild(metaCard);

            const nombresInput = createDomElement("input", "form-control");
            nombresInput.type = "text";
            nombresInput.name = "nombres";
            nombresInput.required = true;
            nombresInput.maxLength = 100;
            nombresInput.value = currentNombres;

            const apellidosInput = createDomElement("input", "form-control");
            apellidosInput.type = "text";
            apellidosInput.name = "apellidos";
            apellidosInput.required = true;
            apellidosInput.maxLength = 100;
            apellidosInput.value = currentApellidos;

            const telefonoInput = createDomElement("input", "form-control");
            telefonoInput.type = "tel";
            telefonoInput.inputMode = "numeric";
            telefonoInput.name = "telefono";
            telefonoInput.maxLength = 9;
            telefonoInput.pattern = "^$|\\d{9}";
            telefonoInput.placeholder = "9 dígitos numéricos";
            telefonoInput.value = currentTelefono;

            const estadoSelect = createDomElement("select", "form-select");
            estadoSelect.name = "activo";
            const optActivo = createDomElement("option", "", "ACTIVO");
            optActivo.value = "true";
            const optInactivo = createDomElement("option", "", "INACTIVO");
            optInactivo.value = "false";
            estadoSelect.append(optActivo, optInactivo);
            estadoSelect.value = currentActivo ? "true" : "false";

            form.append(
                buildLabeledField("editSocioNombres", "NOMBRES *", nombresInput),
                buildLabeledField("editSocioApellidos", "APELLIDOS *", apellidosInput),
                buildLabeledField("editSocioTelefono", "TELÉFONO (9 DÍGITOS)", telefonoInput),
                buildLabeledField("editSocioEstado", "ESTADO DE CUENTA", estadoSelect)
            );

            const actionsRow = createDomElement("div", "d-flex flex-wrap gap-2 mt-4");
            const saveBtn = createDomElement("button", "btn btn-primary flex-grow-1", "");
            saveBtn.type = "submit";
            saveBtn.id = "btnSaveSocioModal";
            saveBtn.append(createIcon("fa-solid fa-floppy-disk me-1"), document.createTextNode("GUARDAR CAMBIOS"));

            const nextActivo = !currentActivo;
            const toggleBtn = createDomElement(
                "button",
                `btn btn-outline ${nextActivo ? "text-success" : "text-danger"}`,
                ""
            );
            toggleBtn.type = "button";
            toggleBtn.id = "btnToggleSocioEstadoModal";
            toggleBtn.append(
                createIcon(nextActivo ? "fa-solid fa-user-check me-1" : "fa-solid fa-user-slash me-1"),
                document.createTextNode(nextActivo ? "ACTIVAR SOCIO" : "DESACTIVAR SOCIO")
            );

            actionsRow.append(saveBtn, toggleBtn);
            form.appendChild(actionsRow);

            const updateRowDom = (socioResp) => {
                const updatedNombres = socioResp.nombres ?? nombresInput.value.trim();
                const updatedApellidos = socioResp.apellidos ?? apellidosInput.value.trim();
                const updatedTel = socioResp.telefono ?? telefonoInput.value.trim();
                const updatedActivo = typeof socioResp.activo === "boolean" ? socioResp.activo : (estadoSelect.value === "true");

                const nameCell = row.querySelector("[data-socio-cell='nombre']") || row.children[1];
                const telCell = row.querySelector("[data-socio-cell='telefono']") || row.children[3];
                const statusCell = row.querySelector("[data-socio-cell='estado']") || row.children[5];

                if (nameCell) nameCell.textContent = `${updatedNombres} ${updatedApellidos}`.trim();
                if (telCell) telCell.textContent = updatedTel ? updatedTel : "—";
                renderSocioStatusCell(statusCell, updatedActivo);

                btn.dataset.nombres = updatedNombres;
                btn.dataset.apellidos = updatedApellidos;
                btn.dataset.telefono = updatedTel;
                btn.dataset.activo = String(updatedActivo);
            };

            const setBusy = (isBusy) => {
                saveBtn.disabled = isBusy;
                toggleBtn.disabled = isBusy;
                nombresInput.disabled = isBusy;
                apellidosInput.disabled = isBusy;
                telefonoInput.disabled = isBusy;
                estadoSelect.disabled = isBusy;
            };

            form.addEventListener("submit", async (ev) => {
                ev.preventDefault();
                feedbackBox.replaceChildren();

                const nombres = nombresInput.value.trim();
                const apellidos = apellidosInput.value.trim();
                const telefono = telefonoInput.value.trim();
                const activo = estadoSelect.value === "true";

                if (!nombres || !apellidos) {
                    const campo = nombres ? apellidosInput : nombresInput;
                    campo.setAttribute("aria-invalid", "true");
                    campo.setAttribute("aria-describedby", feedbackBox.id);
                    campo.addEventListener("input", () => {
                        campo.removeAttribute("aria-invalid");
                        campo.removeAttribute("aria-describedby");
                        feedbackBox.replaceChildren();
                    }, { once: true });
                    feedbackBox.appendChild(buildAlertBanner("Nombres y apellidos son obligatorios."));
                    campo.focus();
                    return;
                }
                if (telefono && !/^\d{9}$/.test(telefono)) {
                    feedbackBox.appendChild(buildAlertBanner("El teléfono debe contener exactamente 9 dígitos numéricos."));
                    return;
                }

                setBusy(true);
                try {
                    const result = await requestSocioUpdate(socioId, { nombres, apellidos, telefono, activo });
                    if (!result.ok || result.data?.success === false) {
                        const errMsg = result.data?.mensaje || result.data?.error || `No se pudo guardar los cambios en el servidor (HTTP ${result.status}).`;
                        feedbackBox.appendChild(buildAlertBanner(errMsg));
                        showGymToast(errMsg, "fa-triangle-exclamation", "error");
                        return;
                    }
                    updateRowDom(result.data?.socio || { nombres, apellidos, telefono, activo });
                    closeGymModal();
                    showGymToast(result.data?.mensaje || "Socio actualizado correctamente en el servidor.", "fa-circle-check");
                } catch {
                    const msg = "Error de conexión con el servidor. No se guardaron los cambios.";
                    feedbackBox.appendChild(buildAlertBanner(msg));
                    showGymToast(msg, "fa-triangle-exclamation", "error");
                } finally {
                    setBusy(false);
                }
            });

            toggleBtn.addEventListener("click", async () => {
                feedbackBox.replaceChildren();
                const targetActivo = btn.dataset.activo === "false";
                const accionTxt = targetActivo ? "activar" : "desactivar";
                if (!window.confirm(`¿Confirmas ${accionTxt} la cuenta del socio?`)) {
                    return;
                }

                setBusy(true);
                try {
                    const result = await requestSocioToggleEstado(socioId, targetActivo);
                    if (!result.ok || result.data?.success === false) {
                        const errMsg = result.data?.mensaje || result.data?.error || `No se pudo cambiar el estado del socio (HTTP ${result.status}).`;
                        feedbackBox.appendChild(buildAlertBanner(errMsg));
                        showGymToast(errMsg, "fa-triangle-exclamation", "error");
                        return;
                    }
                    const confirmedActivo = typeof result.data?.activo === "boolean" ? result.data.activo : targetActivo;
                    updateRowDom({
                        nombres: btn.dataset.nombres,
                        apellidos: btn.dataset.apellidos,
                        telefono: btn.dataset.telefono,
                        activo: confirmedActivo
                    });
                    closeGymModal();
                    showGymToast(
                        result.data?.mensaje || (confirmedActivo ? "Socio activado correctamente." : "Socio desactivado correctamente."),
                        confirmedActivo ? "fa-user-check" : "fa-user-slash"
                    );
                } catch {
                    const msg = "Error de conexión con el servidor. No se modificó el estado del socio.";
                    feedbackBox.appendChild(buildAlertBanner(msg));
                    showGymToast(msg, "fa-triangle-exclamation", "error");
                } finally {
                    setBusy(false);
                }
            });

            openSafeGymModal("EDITAR SOCIO", form, "fa-solid fa-user-pen text-primary me-2");
        });
    });
}

function buildVoucherDetailRow(labelText, valueText, valueClass = "") {
    const row = createDomElement("div", "d-flex justify-content-between gap-2 mb-1");
    row.append(
        createDomElement("span", "text-muted", labelText),
        createDomElement("span", valueClass, valueText)
    );
    return row;
}

export function initAdminCrudModals() {
    // Edición segura y persistente de socios (C04 + C08)
    initSocioEditModal();

    // Binding para modal Editar Plan
    document.querySelectorAll(".btn-edit-plan").forEach((btn) => {
        btn.addEventListener("click", () => {
            const idInput = document.getElementById("planId");
            const nombreInput = document.getElementById("planNombre");
            const precioInput = document.getElementById("planPrecio");
            const duracionInput = document.getElementById("planDuracion");
            const descInput = document.getElementById("planDescripcion");
            const activoInput = document.getElementById("planActivo");
            const title = document.getElementById("modalPlanTitle");

            if (idInput) idInput.value = btn.dataset.id || "";
            if (nombreInput) nombreInput.value = btn.dataset.nombre || "";
            if (precioInput) precioInput.value = btn.dataset.precio || "";
            if (duracionInput) duracionInput.value = btn.dataset.duracion || "";
            if (descInput) descInput.value = btn.dataset.descripcion || "";
            if (activoInput) activoInput.value = btn.dataset.activo || "true";
            if (title) title.textContent = "EDITAR PLAN";
        });
    });

    const btnNewPlan = document.querySelector('[data-bs-target="#modalNuevoPlan"]');
    if (btnNewPlan) {
        btnNewPlan.addEventListener("click", () => {
            const form = document.getElementById("formPlan");
            if (form) form.reset();
            const idInput = document.getElementById("planId");
            if (idInput) idInput.value = "";
            const title = document.getElementById("modalPlanTitle");
            if (title) title.textContent = "NUEVO PLAN";
        });
    }

    // Binding para modal Editar Producto
    document.querySelectorAll(".btn-edit-producto").forEach((btn) => {
        btn.addEventListener("click", () => {
            const idInput = document.getElementById("prodId");
            const nombreInput = document.getElementById("prodNombre");
            const precioInput = document.getElementById("prodPrecio");
            const stockInput = document.getElementById("prodStock");
            const categoriaInput = document.getElementById("prodCategoria");
            const imagenInput = document.getElementById("prodImagen");
            const descInput = document.getElementById("prodDescripcion");
            const activoInput = document.getElementById("prodActivo");
            const title = document.getElementById("modalProdTitle");

            if (idInput) idInput.value = btn.dataset.id || "";
            if (nombreInput) nombreInput.value = btn.dataset.nombre || "";
            if (precioInput) precioInput.value = btn.dataset.precio || "";
            if (stockInput) stockInput.value = btn.dataset.stock || "";
            if (categoriaInput) categoriaInput.value = btn.dataset.categoria || "OTROS";
            if (imagenInput) imagenInput.value = btn.dataset.imagen || "";
            if (descInput) descInput.value = btn.dataset.descripcion || "";
            if (activoInput) activoInput.value = btn.dataset.activo || "true";
            if (title) title.textContent = "EDITAR PRODUCTO";
        });
    });

    const btnNewProd = document.querySelector('[data-bs-target="#modalNuevoProducto"]');
    if (btnNewProd) {
        btnNewProd.addEventListener("click", () => {
            const form = document.getElementById("formProducto");
            if (form) form.reset();
            const idInput = document.getElementById("prodId");
            if (idInput) idInput.value = "";
            const title = document.getElementById("modalProdTitle");
            if (title) title.textContent = "NUEVO PRODUCTO";
        });
    }

    // Binding para modal Editar Promoción
    document.querySelectorAll(".btn-edit-promocion").forEach((btn) => {
        btn.addEventListener("click", () => {
            const idInput = document.getElementById("promoId");
            const tituloInput = document.getElementById("promoTitulo");
            const descInput = document.getElementById("promoDescuento");
            const inicioInput = document.getElementById("promoInicio");
            const finInput = document.getElementById("promoFin");
            const descripcionInput = document.getElementById("promoDescripcion");
            const activaInput = document.getElementById("promoActiva");
            const title = document.getElementById("modalPromoTitle");

            if (idInput) idInput.value = btn.dataset.id || "";
            if (tituloInput) tituloInput.value = btn.dataset.titulo || "";
            if (descInput) descInput.value = btn.dataset.descuento || "";
            if (inicioInput) inicioInput.value = btn.dataset.inicio || "";
            if (finInput) finInput.value = btn.dataset.fin || "";
            if (descripcionInput) descripcionInput.value = btn.dataset.descripcion || "";
            if (activaInput) activaInput.value = btn.dataset.activa || "true";
            if (title) title.textContent = "EDITAR PROMOCIÓN";
        });
    });

    const btnNewPromo = document.querySelector('[data-bs-target="#modalNuevaPromocion"]');
    if (btnNewPromo) {
        btnNewPromo.addEventListener("click", () => {
            const form = document.getElementById("formPromocion");
            if (form) form.reset();
            const idInput = document.getElementById("promoId");
            if (idInput) idInput.value = "";
            const title = document.getElementById("modalPromoTitle");
            if (title) title.textContent = "NUEVA PROMOCIÓN";
        });
    }

    // Binding para modal Editar Equipo
    document.querySelectorAll(".btn-edit-equipo").forEach((btn) => {
        btn.addEventListener("click", () => {
            const idInput = document.getElementById("equipoId");
            const codInput = document.getElementById("equipoCodigo");
            const nomInput = document.getElementById("equipoNombre");
            const estInput = document.getElementById("equipoEstado");
            const title = document.getElementById("modalEquipoTitle");

            if (idInput) idInput.value = btn.dataset.id || "";
            if (codInput) codInput.value = btn.dataset.codigo || "";
            if (nomInput) nomInput.value = btn.dataset.nombre || "";
            if (estInput) estInput.value = btn.dataset.estado || "OPERATIVO";
            if (title) title.textContent = "EDITAR EQUIPO";
        });
    });

    const btnNewEquipo = document.querySelector('[data-bs-target="#modalNuevoEquipo"]');
    if (btnNewEquipo) {
        btnNewEquipo.addEventListener("click", () => {
            const form = document.getElementById("formEquipo");
            if (form) form.reset();
            const idInput = document.getElementById("equipoId");
            if (idInput) idInput.value = "";
            const title = document.getElementById("modalEquipoTitle");
            if (title) title.textContent = "REGISTRAR EQUIPO";
        });
    }

    // Autofill monto de pago según membresía
    const selectMembresia = document.getElementById("selectMembresiaPago");
    if (selectMembresia) {
        selectMembresia.addEventListener("change", function () {
            const selectedOpt = this.options[this.selectedIndex];
            const monto = selectedOpt?.getAttribute("data-monto");
            const inputMonto = document.getElementById("inputMontoPago");
            if (inputMonto && monto) {
                inputMonto.value = parseFloat(monto).toFixed(2);
            }
        });
    }

    // Modal Voucher Comprobante Académico (DOM seguro C04 + M15 + L02)
    document.querySelectorAll(".btn-view-voucher").forEach((btn) => {
        if (btn.dataset.voucherBound) return;
        btn.dataset.voucherBound = "true";
        btn.addEventListener("click", () => {
            const pagoId = btn.dataset.pagoId || "0";
            const socio = btn.dataset.socio || "Socio";
            const dni = btn.dataset.dni || "—";
            const plan = btn.dataset.plan || "Membresía";
            const monto = btn.dataset.monto || "0.00";
            const metodo = btn.dataset.metodo || "—";
            const fecha = btn.dataset.fecha || "—";

            const wrapper = createDomElement("div", "p-2 text-center");
            const voucherBadge = createDomElement(
                "div",
                "badge bg-dark text-warning border border-warning font-anton text-2xs px-3 py-1 mb-2",
                "CONSTANCIA DE PAGO EN RECEPCIÓN"
            );
            const brand = createDomElement("h3", "font-anton text-primary mb-1", "ENERGY GYM");
            const address = createDomElement("p", "text-muted text-xs mb-2", "Sede Av. Las Américas, Ate — Lima");
            const disclaimer = createDomElement(
                "p",
                "text-warning text-2xs mb-3",
                "Documento interno sin validez tributaria."
            );

            const details = createDomElement("div", "text-start mb-3 p-3 rounded bg-elevated border border-secondary text-xs");
            details.append(
                buildVoucherDetailRow("FECHA Y HORA:", fecha, "fw-bold text-light"),
                buildVoucherDetailRow("SOCIO:", socio, "fw-bold text-light"),
                buildVoucherDetailRow("DNI:", dni, "text-light"),
                buildVoucherDetailRow("CONCEPTO:", plan, "text-primary fw-bold"),
                buildVoucherDetailRow("MÉTODO:", metodo, "text-light")
            );

            const totalBox = createDomElement(
                "div",
                "bg-dark p-3 rounded border border-secondary mb-3 d-flex justify-content-between align-items-center"
            );
            totalBox.append(
                createDomElement("span", "font-anton text-light fs-5", "TOTAL REGISTRADO"),
                createDomElement("span", "font-anton text-primary fs-4", `S/ ${monto}`)
            );

            const printBtn = createDomElement("button", "btn btn-outline w-100", "");
            printBtn.type = "button";
            printBtn.append(createIcon("fa-solid fa-print me-1"), document.createTextNode("IMPRIMIR CONSTANCIA"));
            printBtn.addEventListener("click", () => window.print());

            wrapper.append(voucherBadge, brand, address, disclaimer, details, totalBox, printBtn);

            openSafeGymModal(
                `CONSTANCIA DE CAJA N° ${String(pagoId).padStart(5, "0")}`,
                wrapper,
                "fa-solid fa-receipt text-primary me-2"
            );
        });
    });
}
