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
