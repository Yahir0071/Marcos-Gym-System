import { postJson } from "../core/api-client.js";

const days = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"];
const templates = {
    3: [[1, "Pecho / tríceps", "Press de banca: 3 series de 10 repeticiones\nPress inclinado: 3 × 10\nExtensión de tríceps: 3 × 12"],
        [3, "Espalda / bíceps", "Jalón al pecho: 3 × 10\nRemo: 3 × 10\nCurl de bíceps: 3 × 12"],
        [5, "Pierna completa", "Sentadilla: 3 × 10\nPrensa: 3 × 12\nCurl femoral: 3 × 12\nElevación de talones: 3 × 15"]],
    4: [[1, "Torso A", "Press de banca: 3 × 10\nRemo: 3 × 10\nCurl de bíceps: 2 × 12"],
        [2, "Pierna A", "Sentadilla: 3 × 10\nCurl femoral: 3 × 12\nElevación de talones: 3 × 15"],
        [4, "Torso B", "Jalón al pecho: 3 × 10\nPress de hombros: 3 × 10\nExtensión de tríceps: 2 × 12"],
        [5, "Pierna B", "Prensa: 3 × 12\nPuente de glúteos: 3 × 12\nExtensión de piernas: 3 × 12"]],
    5: [[1, "Pecho / tríceps", "Press de banca: 3 × 10\nAperturas: 3 × 12\nExtensión de tríceps: 3 × 12"],
        [2, "Espalda / bíceps", "Jalón al pecho: 3 × 10\nRemo: 3 × 10\nCurl de bíceps: 3 × 12"],
        [3, "Pierna completa", "Sentadilla: 3 × 10\nCurl femoral: 3 × 12\nElevación de talones: 3 × 15"],
        [4, "Hombros / abdomen", "Press de hombros: 3 × 10\nElevaciones laterales: 3 × 12\nPlancha: 3 series de 30 segundos"],
        [5, "Torso", "Press inclinado: 3 × 10\nRemo: 3 × 10\nCurl de bíceps: 2 × 12"]]
};

function element(tag, cls, text) {
    const node = document.createElement(tag); node.className = cls;
    if (text !== undefined) node.textContent = text;
    return node;
}

export async function initWorkoutTracker() {
    const grid = document.getElementById("weeklyWorkoutGrid");
    if (!grid) return;
    const editor = document.getElementById("routineEditor");
    const form = document.getElementById("routineForm");
    const status = document.getElementById("routineStatus");
    const saveStatus = document.getElementById("routineSaveStatus");
    const todayButton = document.getElementById("btnCheckinToday");
    const saveButton = document.getElementById("saveRoutine");
    let state, busy = false, dirty = false;
    const text = (id, value) => { const el = document.getElementById(id); if (el) el.textContent = value; };
    function edit(draft) {
        editor.replaceChildren();
        days.forEach((name, index) => {
            const day = draft.find(d => d.dia === index + 1);
            const col = element("div", "col-md-6 col-xl-4");
            const card = element("div", "card p-3 h-100");
            const label = element("label", "form-check d-flex gap-2 align-items-center mb-3");
            const check = element("input", "form-check-input"); check.type = "checkbox"; check.checked = Boolean(day?.sesion); check.dataset.day = index + 1;
            label.append(check, element("span", "font-anton text-light", name.toUpperCase()));
            const titleLabel = element("label", "form-label text-xs", "SESIÓN"); titleLabel.htmlFor = `routineTitle${index}`;
            const title = element("input", "form-control mb-3"); title.type = "text"; title.id = titleLabel.htmlFor; title.maxLength = 100;
            title.placeholder = "Ej.: Pecho / tríceps"; title.value = day?.sesion || ""; title.disabled = !check.checked; title.required = check.checked;
            const descLabel = element("label", "form-label text-xs", "EJERCICIOS, SERIES Y REPETICIONES"); descLabel.htmlFor = `routineDesc${index}`;
            const desc = element("textarea", "form-control text-sm"); desc.id = descLabel.htmlFor; desc.rows = 5; desc.maxLength = 4000;
            desc.placeholder = "Press de banca: 3 series de 10 repeticiones…"; desc.value = day?.descripcion || ""; desc.disabled = !check.checked;
            check.addEventListener("change", () => { title.disabled = desc.disabled = !check.checked; title.required = check.checked; });
            card.append(label, titleLabel, title, descLabel, desc); col.append(card); editor.append(col);
        });
    }
    function render() {
        grid.replaceChildren();
        state.semana.forEach((d, i) => {
            const btn = element("button", `btn btn-outline py-3 d-flex flex-column align-items-center gap-2 workout-day-btn${d.completado ? " active" : ""}`);
            btn.type = "button"; btn.disabled = busy || !d.sesion || d.fecha > state.hoy;
            btn.setAttribute("aria-pressed", String(d.completado));
            btn.setAttribute("aria-label", `${days[i]} ${d.fecha}: ${d.sesion || "Descanso"}${d.completado ? ", completado" : ""}`);
            const date = `${d.fecha.slice(8, 10)}/${d.fecha.slice(5, 7)}`;
            btn.append(element("span", "text-2xs text-muted", `${d.fecha === state.hoy ? "HOY · " : ""}${days[i].toUpperCase()} ${date}`),
                element("i", d.completado ? "fa-solid fa-circle-check fs-4 text-primary" : !d.sesion ? "fa-solid fa-moon fs-4 text-muted" : "fa-regular fa-circle fs-4 text-muted"),
                element("span", "text-2xs", d.sesion || "DESCANSO"));
            btn.addEventListener("click", () => mark(d)); grid.append(btn);
        });
        text("weeklyTrackerSummary", `${state.completadosSemana} / ${state.metaSemana} DÍAS COMPLETADOS`);
        text("memberAttendanceCount", `${state.completadosMes} entrenamientos`);
        text("memberMonthlyTarget", `Meta mensual: ${state.metaMes} entrenamientos`);
        text("monthlyGoalCount", `${state.completadosMes} / ${state.metaMes}`);
        text("topbarStreakCount", `${state.racha} SESIONES 🔥`);
        const progress = document.getElementById("monthlyGoalProgress");
        progress.style.width = `${state.metaMes ? Math.min(100, state.completadosMes / state.metaMes * 100) : 0}%`;
        progress.parentElement.setAttribute("aria-valuemax", state.metaMes);
        progress.parentElement.setAttribute("aria-valuenow", state.completadosMes);
        const left = state.metaMes - state.completadosMes;
        text("monthlyGoalMessage", !state.metaMes ? "Configura tu rutina para calcular tu meta mensual." : left > 0 ? `Te faltan ${left} entrenamientos para alcanzar tu meta de este mes.` : "¡Meta mensual completada! Sigue con tu rutina.");
        const today = state.semana.find(d => d.fecha === state.hoy);
        todayButton.disabled = busy || !today?.sesion || today.completado;
        todayButton.textContent = today?.completado ? "ENTRENO DE HOY REGISTRADO" : !today?.sesion ? "HOY NO HAY SESIÓN PROGRAMADA" : "REGISTRAR ENTRENO DE HOY";
        status.textContent = state.rutina.length ? "Registro personal de entrenamientos. Los descansos no restan progreso." : "Todavía no tienes una rutina. Configúrala abajo para empezar.";
    }
    async function request(url, body) {
        const response = await postJson(url, body);
        if (!response.ok || response.redirected) {
            let message = "No se pudo guardar. Revisa tu conexión o vuelve a iniciar sesión.";
            try { const error = await response.json(); message = error.detail || error.message || message; } catch { /* Sin respuesta JSON */ }
            throw new Error(message);
        }
        return response.json();
    }
    async function mark(day) {
        if (busy) return;
        busy = true; render();
        try { state = await request("/cliente/api/rutina/registro", {fecha: day.fecha, completado: !day.completado}); }
        catch (e) { status.textContent = e.message; busy = false; render(); status.textContent = e.message; return; }
        busy = false; render();
    }
    form.addEventListener("input", () => { dirty = true; saveStatus.textContent = "Cambios sin guardar"; });
    document.querySelectorAll(".routine-template").forEach(btn => btn.addEventListener("click", () => {
        if (!state || busy) return;
        if (dirty && !window.confirm("¿Reemplazar los cambios sin guardar con esta plantilla?")) return;
        edit(templates[btn.dataset.template].map(([dia, sesion, descripcion]) => ({dia, sesion, descripcion})));
        dirty = true; saveStatus.textContent = "Plantilla cargada. Puedes editarla antes de guardar.";
    }));
    document.getElementById("newCustomRoutine").addEventListener("click", () => {
        if (!state || busy) return;
        if (dirty && !window.confirm("¿Descartar los cambios sin guardar y empezar una rutina propia?")) return;
        edit([]); dirty = true; saveStatus.textContent = "Selecciona al menos un día y escribe su sesión.";
    });
    document.getElementById("restoreRoutine").addEventListener("click", () => { if (!state || busy) return; edit(state.rutina); dirty = false; saveStatus.textContent = "Cambios descartados."; });
    form.addEventListener("submit", async e => {
        e.preventDefault(); if (!state || busy) return;
        const draft = [...editor.children].map((col, i) => ({dia: i + 1,
            sesion: col.querySelector('input[type="checkbox"]').checked ? col.querySelector('input[type="text"]').value.trim() : "",
            descripcion: col.querySelector("textarea").value.trim()}));
        if (!draft.some(d => d.sesion)) { saveStatus.textContent = "Selecciona al menos un día de entrenamiento."; return; }
        busy = true; saveButton.disabled = true; render();
        editor.querySelectorAll("input, textarea").forEach(el => el.disabled = true);
        try { state = await request("/cliente/api/rutina", draft); dirty = false; saveStatus.textContent = "Rutina guardada. Vigente desde hoy."; }
        catch (error) { saveStatus.textContent = error.message; }
        finally { busy = false; saveButton.disabled = false; edit(draft); render(); }
    });
    todayButton.addEventListener("click", () => { const d = state?.semana.find(d => d.fecha === state.hoy); if (d?.sesion && !d.completado) mark(d); });
    window.addEventListener("beforeunload", e => { if (dirty) { e.preventDefault(); e.returnValue = ""; } });
    try {
        form.querySelectorAll("button").forEach(b => b.disabled = true);
        const response = await fetch("/cliente/api/rutina");
        if (!response.ok || response.redirected) throw new Error("No se pudo cargar tu rutina. Recarga la página o inicia sesión nuevamente.");
        state = await response.json(); edit(state.rutina); render();
        form.querySelectorAll("button").forEach(b => b.disabled = false);
    } catch (e) { status.textContent = e.message; todayButton.disabled = true; }
}
