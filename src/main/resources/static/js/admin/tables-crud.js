/**
 * admin/tables-crud.js - Energy Gym Administration Engine
 * Responsabilidad: Filtrado universal de tablas, gráficos del dashboard, pestañas y modales CRUD.
 */

import { openGymModal } from "../core/modal.js";
import { showGymToast } from "../core/toast.js";

export function initTableFilters() {
    const applyTableFilters = () => {
        const table = document.querySelector(".filterable-table");
        if (!table) return;
        const searchInput = document.querySelector(".table-search-input");
        const query = (searchInput ? searchInput.value : "").toLowerCase().trim();
        const selects = Array.from(document.querySelectorAll(".table-filter-select"));

        table.querySelectorAll("tbody tr").forEach((row) => {
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
    const chartPeriodBtns = document.querySelectorAll(".chart-period-btn");
    if (chartPeriodBtns.length === 0) return;

    const chartData = {
        semana: [
            { val: "115", h: "72%" },
            { val: "132", h: "84%" },
            { val: "128", h: "80%" },
            { val: "145", h: "92%" },
            { val: "138", h: "87%" },
            { val: "96", h: "60%" },
            { val: "54", h: "35%" }
        ],
        mes: [
            { val: "480", h: "78%" },
            { val: "520", h: "85%" },
            { val: "510", h: "82%" },
            { val: "590", h: "95%" },
            { val: "540", h: "88%" },
            { val: "410", h: "66%" },
            { val: "230", h: "42%" }
        ]
    };

    chartPeriodBtns.forEach((btn) => {
        btn.addEventListener("click", function () {
            chartPeriodBtns.forEach((b) => {
                b.classList.remove("btn-primary", "active");
                b.classList.add("btn-outline");
            });
            this.classList.remove("btn-outline");
            this.classList.add("btn-primary", "active");

            const p = this.getAttribute("data-period") || "semana";
            const data = chartData[p];
            const vals = document.querySelectorAll("#dashboardBarChart .bar-val");
            const fills = document.querySelectorAll("#dashboardBarChart .bar-fill");
            if (data) {
                data.forEach((d, idx) => {
                    if (vals[idx]) vals[idx].textContent = d.val;
                    if (fills[idx]) fills[idx].style.height = d.h;
                });
            }
        });
    });
}

export function initAdminTabs() {
    document.querySelectorAll(".tab-btn[data-tab-target]").forEach((btn) => {
        btn.addEventListener("click", function () {
            document.querySelectorAll(".tab-btn[data-tab-target]").forEach((b) => b.classList.remove("active"));
            this.classList.add("active");
            const target = this.getAttribute("data-tab-target");
            document.querySelectorAll(".admin-tab-pane").forEach((pane) => {
                pane.style.display = target === "all" || pane.id === target ? "" : "none";
            });
        });
    });
}

export function initAdminCrudModals() {
    const bindAdminRowEdits = () => {
        document.querySelectorAll(".btn-edit-row").forEach((btn) => {
            if (btn.dataset.bound) return;
            btn.dataset.bound = "true";
            btn.addEventListener("click", function (e) {
                e.preventDefault();
                const row = this.closest("tr");
                if (!row) return;
                const table = row.closest("table");
                if (!table) return;
                const headers = Array.from(table.querySelectorAll("thead th")).map((th) => th.textContent.trim());
                const cells = Array.from(row.children);

                let fieldsHtml = "";
                for (let i = 0; i < cells.length - 1; i++) {
                    const label = headers[i] || `CAMPO ${i + 1}`;
                    if (label === "IMAGEN") continue;
                    const val = cells[i].textContent.trim();
                    if (label === "ESTADO") {
                        fieldsHtml += `
                            <div class="mb-3">
                                <label class="form-label">${label}</label>
                                <select class="form-select edit-row-input" data-col-idx="${i}" data-is-status="true">
                                    <option value="${val}" selected>${val} (Actual)</option>
                                    <option value="ACTIVO">ACTIVO</option>
                                    <option value="ACTIVA">ACTIVA</option>
                                    <option value="INACTIVO">INACTIVO</option>
                                    <option value="CONFIRMADA">CONFIRMADA</option>
                                    <option value="ENTREGADA">ENTREGADA</option>
                                    <option value="OPERATIVO">OPERATIVO</option>
                                    <option value="EN MANTENIMIENTO">EN MANTENIMIENTO</option>
                                    <option value="FINALIZADO">FINALIZADO</option>
                                    <option value="PUBLICADA">PUBLICADA</option>
                                </select>
                            </div>`;
                    } else {
                        fieldsHtml += `
                            <div class="mb-3">
                                <label class="form-label">${label}</label>
                                <input type="text" class="form-control edit-row-input" data-col-idx="${i}" value="${val}">
                            </div>`;
                    }
                }

                openGymModal(
                    `<i class="fa-solid fa-pen-to-square me-2"></i> EDITAR REGISTRO`,
                    `<form id="adminEditRowForm">
                        ${fieldsHtml}
                        <div class="d-flex gap-2 mt-4">
                            <button type="submit" class="btn btn-primary flex-grow-1">GUARDAR CAMBIOS</button>
                            <button type="button" class="btn btn-outline text-danger" id="btnDeleteRowModal">ELIMINAR</button>
                        </div>
                    </form>`
                );

                setTimeout(() => {
                    const form = document.getElementById("adminEditRowForm");
                    if (form) {
                        form.addEventListener("submit", (ev) => {
                            ev.preventDefault();
                            form.querySelectorAll(".edit-row-input").forEach((inp) => {
                                const idx = parseInt(inp.getAttribute("data-col-idx"), 10);
                                const isStatus = inp.getAttribute("data-is-status") === "true";
                                const newVal = inp.value.trim();
                                if (isStatus) {
                                    let badgeClass = "";
                                    if (["INACTIVO", "CANCELADA", "FUERA DE SERVICIO", "VENCIDA"].includes(newVal)) badgeClass = "status-inactive";
                                    else if (["EN MANTENIMIENTO", "EN PROCESO", "PENDIENTE", "POR VENCER", "STOCK BAJO"].includes(newVal)) badgeClass = "status-warning";
                                    cells[idx].innerHTML = `<span class="${badgeClass}">${newVal}</span>`;
                                } else {
                                    cells[idx].textContent = newVal;
                                }
                            });
                            if (window.bootstrap && window.bootstrap.Modal) {
                                const modal = window.bootstrap.Modal.getInstance(document.getElementById("dynamicGymModal"));
                                if (modal) modal.hide();
                            }
                            showGymToast("Registro actualizado correctamente", "fa-check");
                        });
                    }
                    const delBtn = document.getElementById("btnDeleteRowModal");
                    if (delBtn) {
                        delBtn.addEventListener("click", () => {
                            row.remove();
                            if (window.bootstrap && window.bootstrap.Modal) {
                                const modal = window.bootstrap.Modal.getInstance(document.getElementById("dynamicGymModal"));
                                if (modal) modal.hide();
                            }
                            showGymToast("Registro eliminado de la tabla", "fa-trash");
                        });
                    }
                }, 50);
            });
        });
    };
    bindAdminRowEdits();

    // Trigger para botones "+ NUEVO"
    document.querySelectorAll("[data-admin-modal]").forEach((btn) => {
        btn.addEventListener("click", function () {
            const type = this.getAttribute("data-admin-modal") || "REGISTRO";
            const table = document.querySelector(".custom-table");
            if (!table) return;
            const headers = Array.from(table.querySelectorAll("thead th")).map((th) => th.textContent.trim());

            let formFields = "";
            headers.forEach((h, idx) => {
                if (h === "ACCIÓN" || h === "IMAGEN") return;
                formFields += `
                    <div class="mb-3">
                        <label class="form-label">${h}</label>
                        <input type="text" class="form-control new-admin-field" data-header="${h}" data-idx="${idx}" placeholder="Ingrese ${h.toLowerCase()}..." required>
                    </div>`;
            });

            openGymModal(
                `<i class="fa-solid fa-plus-circle me-2"></i> NUEVO REGISTRO (${type.toUpperCase()})`,
                `<form id="adminCreateRowForm">
                    ${formFields}
                    <button type="submit" class="btn btn-primary w-100 py-3 mt-2">REGISTRAR AHORA</button>
                </form>`
            );

            setTimeout(() => {
                const form = document.getElementById("adminCreateRowForm");
                if (form) {
                    form.addEventListener("submit", (ev) => {
                        ev.preventDefault();
                        const inputs = Array.from(form.querySelectorAll(".new-admin-field"));
                        const tr = document.createElement("tr");
                        let rowHtml = "";

                        headers.forEach((h) => {
                            if (h === "IMAGEN") {
                                rowHtml += `<td><div class="image-placeholder"><i class="fa-solid fa-box text-primary"></i></div></td>`;
                            } else if (h === "ACCIÓN") {
                                rowHtml += `<td><a href="#" class="btn-edit-row">VER / EDITAR</a></td>`;
                            } else {
                                const match = inputs.find((inp) => inp.getAttribute("data-header") === h);
                                const val = match ? match.value.trim() : "—";
                                if (h === "ESTADO") {
                                    rowHtml += `<td><span>${val.toUpperCase()}</span></td>`;
                                } else if (h === "CÓDIGO" || h === "N°") {
                                    rowHtml += `<td class="code-cell">${val}</td>`;
                                } else {
                                    rowHtml += `<td class="fw-bold">${val}</td>`;
                                }
                            }
                        });

                        tr.innerHTML = rowHtml;
                        const tbody = table.querySelector("tbody");
                        if (tbody) tbody.prepend(tr);
                        bindAdminRowEdits();
                        if (window.bootstrap && window.bootstrap.Modal) {
                            const modal = window.bootstrap.Modal.getInstance(document.getElementById("dynamicGymModal"));
                            if (modal) modal.hide();
                        }
                        showGymToast(`Nuevo registro (${type}) añadido con éxito`, "fa-circle-check");
                    });
                }
            }, 50);
        });
    });

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
            const imagenInput = document.getElementById("prodImagen");
            const descInput = document.getElementById("prodDescripcion");
            const activoInput = document.getElementById("prodActivo");
            const title = document.getElementById("modalProdTitle");

            if (idInput) idInput.value = btn.dataset.id || "";
            if (nombreInput) nombreInput.value = btn.dataset.nombre || "";
            if (precioInput) precioInput.value = btn.dataset.precio || "";
            if (stockInput) stockInput.value = btn.dataset.stock || "";
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
            const monto = selectedOpt.getAttribute("data-monto");
            const inputMonto = document.getElementById("inputMontoPago");
            if (inputMonto && monto) {
                inputMonto.value = parseFloat(monto).toFixed(2);
            }
        });
    }

    // Modal Voucher Comprobante
    document.querySelectorAll(".btn-view-voucher").forEach((btn) => {
        btn.addEventListener("click", () => {
            const pagoId = btn.dataset.pagoId;
            const socio = btn.dataset.socio;
            const dni = btn.dataset.dni;
            const plan = btn.dataset.plan;
            const monto = btn.dataset.monto;
            const metodo = btn.dataset.metodo;
            const fecha = btn.dataset.fecha;

            openGymModal(
                `<i class="fa-solid fa-receipt text-primary me-2"></i> COMPROBANTE DE CAJA N° ${String(pagoId).padStart(5, "0")}`,
                `<div class="p-3 text-center">
                    <h3 class="font-anton text-primary mb-1">ENERGY GYM S.A.C.</h3>
                    <p class="text-muted text-xs mb-3">RUC 20601234567 · Av. Principal 123, Lima</p>
                    <hr class="border-secondary my-3">
                    <div class="text-start mb-3">
                        <div class="d-flex justify-content-between mb-1">
                            <span class="text-muted">FECHA Y HORA:</span>
                            <span class="fw-bold">${fecha}</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1">
                            <span class="text-muted">SOCIO:</span>
                            <span class="fw-bold">${socio}</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1">
                            <span class="text-muted">DNI:</span>
                            <span>${dni}</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1">
                            <span class="text-muted">CONCEPTO:</span>
                            <span class="text-primary fw-bold">${plan}</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1">
                            <span class="text-muted">MÉTODO:</span>
                            <span>${metodo}</span>
                        </div>
                    </div>
                    <div class="bg-dark p-3 rounded border border-secondary mb-3 d-flex justify-content-between align-items-center">
                        <span class="font-anton text-light fs-5">TOTAL PAGADO</span>
                        <span class="font-anton text-primary fs-4">S/ ${monto}</span>
                    </div>
                    <button type="button" class="btn btn-outline w-100" onclick="window.print()">
                        <i class="fa-solid fa-print me-1"></i> IMPRIMIR COMPROBANTE
                    </button>
                </div>`
            );
        });
    });
}
