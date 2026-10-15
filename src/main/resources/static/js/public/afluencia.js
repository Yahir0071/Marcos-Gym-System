/**
 * public/afluencia.js - Energy Gym Live Occupancy / Afluencia Viewer
 * Responsabilidad: Selector de días, visualización de concurrencia y recomendación de franjas horarias.
 */

export function initAfluenciaSelector() {
    const dayBtns = document.querySelectorAll(".day-afluencia-btn");
    if (dayBtns.length === 0) return;

    const formatLevel = (val) => {
        if (val >= 80) return { text: "AFLUENCIA ALTA", cls: "text-danger" };
        if (val >= 55) return { text: "AFLUENCIA MODERADA", cls: "text-primary" };
        return { text: "AFLUENCIA BAJA · IDEAL", cls: "text-success" };
    };

    dayBtns.forEach((btn) => {
        btn.addEventListener("click", function () {
            dayBtns.forEach((b) => {
                b.classList.remove("btn-primary", "active");
                b.classList.add("btn-outline");
            });
            this.classList.remove("btn-outline");
            this.classList.add("btn-primary", "active");

            const day = this.getAttribute("data-day");
            const m = parseInt(this.getAttribute("data-m"), 10) || 50;
            const md = parseInt(this.getAttribute("data-md"), 10) || 70;
            const t = parseInt(this.getAttribute("data-t"), 10) || 80;

            const dayLbl = document.getElementById("selectedDayLabel");
            if (dayLbl) dayLbl.textContent = `PROYECCIÓN PARA: ${day}`;

            const updateBar = (barId, valId, lblId, val) => {
                const bar = document.getElementById(barId);
                const vEl = document.getElementById(valId);
                const lEl = document.getElementById(lblId);
                const info = formatLevel(val);
                if (bar) bar.style.width = `${val}%`;
                if (vEl) {
                    vEl.textContent = `${val}%`;
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
                const minVal = Math.min(m, md, t);
                if (minVal === m) bestEl.textContent = `06:00 – 11:00 AM (${m}% OCUPACIÓN)`;
                else if (minVal === md) bestEl.textContent = `11:00 – 16:00 PM (${md}% OCUPACIÓN)`;
                else bestEl.textContent = `18:00 – 23:00 PM (${t}% OCUPACIÓN)`;
            }
        });
    });
}
