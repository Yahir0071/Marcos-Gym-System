/**
 * public/afluencia.js - Energy Gym Live Occupancy / Afluencia Viewer (M07 / L02 / M06)
 * Responsabilidad: Alternar entre datos reales vigentes registrados en BD y proyecciones referenciales por día.
 */

export function initAfluenciaSelector() {
    const dayBtns = document.querySelectorAll(".day-afluencia-btn");
    if (dayBtns.length === 0) return;

    const formatLevel = (val) => {
        if (val <= 0) return { text: "SIN REGISTROS", cls: "text-muted" };
        if (val >= 80) return { text: "AFLUENCIA ALTA", cls: "text-danger" };
        if (val >= 55) return { text: "AFLUENCIA MODERADA", cls: "text-primary" };
        return { text: "AFLUENCIA BAJA · IDEAL", cls: "text-success" };
    };

    dayBtns.forEach((btn) => {
        btn.setAttribute("aria-pressed", btn.classList.contains("active") ? "true" : "false");

        btn.addEventListener("click", function () {
            dayBtns.forEach((b) => {
                b.classList.remove("btn-primary", "active");
                b.classList.add("btn-outline");
                b.setAttribute("aria-pressed", "false");
            });
            this.classList.remove("btn-outline");
            this.classList.add("btn-primary", "active");
            this.setAttribute("aria-pressed", "true");

            const day = this.getAttribute("data-day") || "VIGENTE";
            const isLive = this.getAttribute("data-live") === "true";
            const m = parseInt(this.getAttribute("data-m") || "0", 10);
            const md = parseInt(this.getAttribute("data-md") || "0", 10);
            const t = parseInt(this.getAttribute("data-t") || "0", 10);

            const dayLbl = document.getElementById("selectedDayLabel");
            if (dayLbl) {
                dayLbl.textContent = isLive
                    ? "REGISTRO VIGENTE EN SISTEMA (DATOS REALES)"
                    : `AFLUENCIA ESTIMADA: ${day}`;
            }

            const updateBar = (barId, valId, lblId, val) => {
                const safeVal = Math.max(0, Math.min(100, isNaN(val) ? 0 : val));
                const bar = document.getElementById(barId);
                const vEl = document.getElementById(valId);
                const lEl = document.getElementById(lblId);
                const info = formatLevel(safeVal);
                if (bar) {
                    bar.style.width = `${safeVal}%`;
                    bar.setAttribute("aria-valuenow", String(safeVal));
                }
                if (vEl) {
                    vEl.textContent = `${safeVal}%`;
                    vEl.className = `font-anton m-0 ${info.cls}`;
                }
                if (lEl) {
                    lEl.textContent = info.text;
                    lEl.className = `text-xs fw-bold ${info.cls}`;
                }
            };

            updateBar("barManana", "valManana", "lblManana", m);
            updateBar("barMediodia", "valMediodia", "lblMediodia", md);
            updateBar("barTarde", "valTarde", "lblTarde", t);

            const bestEl = document.getElementById("bestTimeSlotText");
            if (bestEl) {
                if (m === 0 && md === 0 && t === 0) {
                    bestEl.textContent = "SIN REGISTROS RECIENTES";
                } else {
                    const minVal = Math.min(m, md, t);
                    if (minVal === m) bestEl.textContent = `06:00 – 11:00 (${m}% OCUPACIÓN)`;
                    else if (minVal === md) bestEl.textContent = `11:00 – 16:00 (${md}% OCUPACIÓN)`;
                    else bestEl.textContent = `16:00 – 23:00 (${t}% OCUPACIÓN)`;
                }
            }
        });
    });
}
