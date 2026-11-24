/**
 * core/chatbot.js - Asistente Virtual Freemium de Energy Gym
 * Renderiza estrictamente con textContent (sin innerHTML) y usa CSRF vía api-client.js.
 */
import { getJson, postJson } from "./api-client.js";

export function initChatbot() {
    const wrapper = document.getElementById("gymChatbotWrapper");
    const toggleBtn = document.getElementById("gymChatbotToggle");
    const closeBtn = document.getElementById("gymChatbotClose");
    const panel = document.getElementById("gymChatbotPanel");
    const messagesEl = document.getElementById("gymChatbotMessages");
    const suggestionsEl = document.getElementById("gymChatbotSuggestions");
    const form = document.getElementById("gymChatbotForm");
    const input = document.getElementById("gymChatbotInput");
    const sendBtn = document.getElementById("gymChatbotSend");
    const modeBadge = document.getElementById("gymChatbotModeBadge");

    if (!wrapper || !toggleBtn || !panel || !messagesEl || !form || !input) return;

    const ruta = window.location.pathname;
    if (ruta === "/login" || ruta.startsWith("/admin") || ruta.startsWith("/tecnico")) {
        wrapper.remove();
        return;
    }

    let inicializado = false;
    let enviando = false;

    function actualizarBadge(datos) {
        if (!modeBadge) return;
        if (datos.iaDisponible) {
            const extra = typeof datos.consultasRestantesIa === "number"
                ? ` · ${datos.consultasRestantesIa} hoy`
                : "";
            modeBadge.textContent = `COACH IA ⚡${extra}`;
        } else {
            modeBadge.textContent = "ASISTENTE GYM";
        }
    }

    function agregarMensaje(texto, remitente, enlaces = []) {
        const bubble = document.createElement("div");
        bubble.className = `gym-chatbot-msg gym-chatbot-msg-${remitente}`;

        const body = document.createElement("div");
        body.className = "gym-chatbot-msg-text";
        body.textContent = texto;
        bubble.appendChild(body);

        if (Array.isArray(enlaces) && enlaces.length > 0) {
            const linksWrap = document.createElement("div");
            linksWrap.className = "gym-chatbot-links mt-2 d-flex flex-wrap gap-1";
            enlaces.forEach((item) => {
                if (!item || typeof item.url !== "string" || !item.url.startsWith("/")) return;
                const a = document.createElement("a");
                a.href = item.url;
                a.className = "btn btn-outline btn-sm py-0 px-2 text-2xs";
                a.textContent = item.texto || "Ver más";
                linksWrap.appendChild(a);
            });
            if (linksWrap.childElementCount > 0) {
                bubble.appendChild(linksWrap);
            }
        }

        messagesEl.appendChild(bubble);
        messagesEl.scrollTop = messagesEl.scrollHeight;
        return bubble;
    }

    function renderSugerencias(sugerencias) {
        if (!suggestionsEl) return;
        suggestionsEl.replaceChildren();
        if (!Array.isArray(sugerencias)) return;
        sugerencias.slice(0, 4).forEach((texto) => {
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "gym-chatbot-chip";
            btn.textContent = texto;
            btn.addEventListener("click", () => enviarConsulta(texto));
            suggestionsEl.appendChild(btn);
        });
    }

    async function cargarBienvenida() {
        if (inicializado) return;
        inicializado = true;
        const cargando = agregarMensaje("Conectando con el asistente…", "bot");
        try {
            const resp = await getJson("/api/chatbot/bienvenida");
            const datos = await resp.json().catch(() => ({}));
            cargando.remove();
            if (resp.ok && datos.success) {
                actualizarBadge(datos);
                agregarMensaje(datos.respuesta, "bot", datos.enlaces);
                renderSugerencias(datos.sugerencias);
            } else {
                agregarMensaje("¡Hola! Pregúntame sobre planes, horarios, afluencia o suplementos.", "bot");
            }
        } catch {
            cargando.remove();
            agregarMensaje("No se pudo iniciar el asistente en este momento.", "bot");
        }
    }

    async function enviarConsulta(textoRaw) {
        const texto = (textoRaw || "").trim();
        if (!texto || enviando) return;
        enviando = true;
        input.value = "";
        input.disabled = true;
        if (sendBtn) sendBtn.disabled = true;

        agregarMensaje(texto, "user");
        const escribiendo = agregarMensaje("Escribiendo…", "bot");

        try {
            const resp = await postJson("/api/chatbot/mensaje", { mensaje: texto });
            const datos = await resp.json().catch(() => ({}));
            escribiendo.remove();
            if (resp.ok && datos.success && datos.respuesta) {
                actualizarBadge(datos);
                agregarMensaje(datos.respuesta, "bot", datos.enlaces);
                renderSugerencias(datos.sugerencias);
            } else {
                agregarMensaje(datos.mensaje || "No pude procesar tu consulta. Inténtalo de nuevo.", "bot");
            }
        } catch {
            escribiendo.remove();
            agregarMensaje("Error de conexión. Inténtalo nuevamente.", "bot");
        } finally {
            enviando = false;
            input.disabled = false;
            if (sendBtn) sendBtn.disabled = false;
            input.focus();
        }
    }

    function alternarPanel(abrir) {
        const mostrar = typeof abrir === "boolean" ? abrir : panel.classList.contains("d-none");
        panel.classList.toggle("d-none", !mostrar);
        toggleBtn.setAttribute("aria-expanded", String(mostrar));
        if (mostrar) {
            cargarBienvenida();
            setTimeout(() => input.focus(), 50);
        }
    }

    toggleBtn.addEventListener("click", () => alternarPanel());
    closeBtn?.addEventListener("click", () => alternarPanel(false));
    form.addEventListener("submit", (e) => {
        e.preventDefault();
        enviarConsulta(input.value);
    });
}
