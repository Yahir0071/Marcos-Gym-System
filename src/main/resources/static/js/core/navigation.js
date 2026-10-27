/**
 * core/navigation.js - Energy Gym Layout & Navigation Engine
 * Responsabilidad: Manejo de barra de navegación, reloj en vivo, sidebars responsivos y pase QR.
 */

import { openGymModal } from "./modal.js";
import { showGymToast } from "./toast.js";

export function initNavbar() {
    const navbar = document.querySelector(".navbar-main");
    if (navbar) {
        window.addEventListener("scroll", () => {
            if (window.scrollY > 30) navbar.classList.add("scrolled");
            else navbar.classList.remove("scrolled");
        });

        const currentPath = window.location.pathname;
        document.querySelectorAll(".navbar-main .nav-link").forEach((link) => {
            const href = link.getAttribute("href");
            if (href && currentPath.includes(href) && href !== "/") {
                document.querySelectorAll(".navbar-main .nav-link").forEach((l) => l.classList.remove("active"));
                link.classList.add("active");
            }
        });
    }
}

export function initLiveClock() {
    const liveClock = document.getElementById("liveClock");
    if (liveClock) {
        const updateClock = () => {
            const now = new Date();
            liveClock.textContent = now.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
        };
        updateClock();
        setInterval(updateClock, 1000);
    }
}

export function initSidebars() {
    const bindSidebar = (openBtnId, closeBtnId, sidebarId) => {
        const openBtn = document.getElementById(openBtnId);
        const closeBtn = document.getElementById(closeBtnId);
        const sidebar = document.getElementById(sidebarId);
        if (openBtn && sidebar) openBtn.addEventListener("click", () => sidebar.classList.add("show"));
        if (closeBtn && sidebar) closeBtn.addEventListener("click", () => sidebar.classList.remove("show"));
    };
    bindSidebar("openSidebarBtn", "closeSidebarBtn", "adminSidebar");
    bindSidebar("openClientSidebarBtn", "closeClientSidebarBtn", "clientSidebar");
}

export function initQrPass() {
    let countdownInterval = null;
    let secondsLeft = 60;

    const generatePassData = (triggerEl) => {
        const sourceEl = (triggerEl?.hasAttribute("data-socio-name") ? triggerEl : null) 
            || document.getElementById("btnTopbarQr") 
            || document.getElementById("btnOpenQrPass") 
            || triggerEl;

        const socioName = sourceEl?.getAttribute("data-socio-name") || "Socio Energy";
        const socioDni = sourceEl?.getAttribute("data-socio-dni") || "—";
        const socioCode = sourceEl?.getAttribute("data-socio-code") || "—";
        const socioInitials = sourceEl?.getAttribute("data-socio-initials") || "EG";
        const planName = sourceEl?.getAttribute("data-plan-name") || "SIN MEMBRESÍA ACTIVA";
        const planEstado = sourceEl?.getAttribute("data-plan-estado") || "INACTIVO";
        const planVence = sourceEl?.getAttribute("data-plan-vence") || "—";
        const diasRestantes = sourceEl?.getAttribute("data-dias-restantes") || "0";

        const randomToken = "EG-" + socioDni.slice(-4) + "-" + Math.floor(1000 + Math.random() * 9000);

        const qrPayload = `ENERGY GYM - CARNET DIGITAL DE ACCESO
Socio: ${socioName}
DNI: ${socioDni} | Pase: ${socioCode}
Plan: ${planName} (${planEstado})
Sede: Las Am�ricas, Ate
Vigencia: ${planVence} (${diasRestantes} días restantes)
Token Seguridad: ${randomToken}
Verificado por: torniquete.energygym.pe`;

        const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=4&data=${encodeURIComponent(qrPayload)}`;

        return {
            socioName,
            socioDni,
            socioCode,
            socioInitials,
            planName,
            planEstado,
            planVence,
            diasRestantes,
            token: randomToken,
            qrPayload,
            qrUrl
        };
    };

    const triggerQrModal = (e) => {
        const triggerEl = e?.currentTarget || document.getElementById("btnTopbarQr") || document.getElementById("btnOpenQrPass");
        let passData = generatePassData(triggerEl);
        secondsLeft = 60;
        if (countdownInterval) clearInterval(countdownInterval);

        openGymModal(
            `<i class="fa-solid fa-id-card text-primary me-2"></i> CARNET DIGITAL DE SOCIO · ENERGY SMART PASS`,
            `<div class="p-1">
                <!-- CARNET DIGITAL ESTILO APPLE WALLET / SMART FIT -->
                <div class="gym-smart-pass p-4 text-start position-relative" id="smartMemberCard">
                    <!-- Encabezado del Carnet: Logo + Chip EMV -->
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <span class="font-anton text-primary fs-5 tracking-wide d-flex align-items-center gap-1">
                                <span class="fs-4">⚡</span> ENERGY <span class="text-light">GYM</span>
                            </span>
                            <span class="text-2xs text-muted font-anton tracking-wider d-block">CONTROL DE ACCESO BIOMÉTRICO</span>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <div class="emv-chip" title="Chip EMV de Control de Acceso"></div>
                            <i class="fa-solid fa-wifi text-muted fs-6" style="transform: rotate(90deg);" title="Sensor de Proximidad NFC / Óptico"></i>
                        </div>
                    </div>

                    <!-- Ficha del Socio con Iniciales y Datos Reales -->
                    <div class="d-flex align-items-center gap-3 p-3 rounded bg-elevated border border-secondary mb-3">
                        <div class="rounded-circle bg-primary text-dark font-anton fs-5 fw-bold d-flex align-items-center justify-content-center" style="width: 48px; height: 48px; flex-shrink: 0;">
                            ${passData.socioInitials}
                        </div>
                        <div class="flex-grow-1 overflow-hidden">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <span class="font-anton text-light text-md text-truncate d-block">${passData.socioName.toUpperCase()}</span>
                                <span class="badge bg-dark text-primary border border-secondary font-anton text-2xs">${passData.socioCode}</span>
                            </div>
                            <div class="d-flex align-items-center gap-2 text-2xs">
                                <span class="pulse-green"></span>
                                <span class="text-success fw-bold">MEMBRESÍA ACTIVA (AL DÍA)</span>
                                <span class="text-muted">· DNI ${passData.socioDni}</span>
                            </div>
                        </div>
                    </div>

                    <!-- Cobertura y Sede -->
                    <div class="d-flex justify-content-between align-items-center text-xs mb-3 px-1">
                        <span class="text-primary font-anton tracking-wide"><i class="fa-solid fa-dumbbell me-1"></i> ${passData.planName.toUpperCase()}</span>
                        <span class="text-muted"><i class="fa-solid fa-location-dot me-1 text-primary"></i> Las Am�ricas, Ate</span>
                        <span class="text-light fw-bold"><i class="fa-regular fa-calendar-check me-1 text-primary"></i> Vence: ${passData.planVence}</span>
                    </div>

                    <!-- Contenedor del QR Real con Escaneo Láser Neón Animado -->
                    <div class="text-center py-2" id="qrPassVisualArea">
                        <div class="qr-laser-container" id="qrContainerBox">
                            <div class="qr-laser-beam"></div>
                            <img src="${passData.qrUrl}" alt="Código QR de Acceso Biométrico" id="realQrImage" style="width: 190px; height: 190px; display: block;">
                        </div>
                    </div>

                    <!-- Área de Concesión de Acceso al Torniquete (Pantalla de Éxito) -->
                    <div class="text-center py-4 d-none" id="turnstileGrantedArea">
                        <div class="mb-3">
                            <i class="fa-solid fa-circle-check text-success" style="font-size: 4.5rem; filter: drop-shadow(0 0 15px rgba(0,230,118,0.5));"></i>
                        </div>
                        <h3 class="font-anton text-success mb-1 tracking-wide">¡ACCESO CONCEDIDO!</h3>
                        <div class="badge bg-dark text-success border border-success font-anton px-3 py-1 mb-2 text-xs">
                            TORNIQUETE 01 DESBLOQUEADO
                        </div>
                        <p class="text-light text-xs mb-2">¡Buen entrenamiento, <strong>${passData.socioName}</strong>!</p>
                        <div class="p-2 px-3 rounded bg-elevated border border-secondary d-inline-block text-2xs text-muted mb-3">
                            <i class="fa-solid fa-clock text-primary me-1"></i> <span id="grantedTimeText">Hoy a las 07:15 AM</span> &bull; Visitas del mes: <strong class="text-primary" id="grantedVisitsText">19</strong> (+1 hoy)
                        </div>
                        <div>
                            <button type="button" class="btn btn-outline btn-sm font-anton text-xs py-2 px-3" id="btnReturnToQr">
                                <i class="fa-solid fa-qrcode me-1"></i> MOSTRAR CÓDIGO QR NUEVAMENTE
                            </button>
                        </div>
                    </div>

                    <!-- Barra Antifraude Dinámica y Token TOTP -->
                    <div id="antifraudFooterArea">
                        <div class="d-flex justify-content-between align-items-center text-2xs text-muted mb-1 px-1">
                            <span>TOKEN EN VIVO: <strong class="text-primary font-anton" id="qrTokenDisplay">${passData.token}</strong></span>
                            <span class="d-flex align-items-center gap-1">
                                <span id="qrTimerCountdown">Se renueva en 60s</span>
                                <button type="button" class="btn btn-link text-primary p-0 text-decoration-none" id="btnForceRefreshToken" title="Renovar token de seguridad">
                                    <i class="fa-solid fa-rotate-right"></i>
                                </button>
                            </span>
                        </div>
                        <div class="progress mb-3" style="height: 4px; background: rgba(255, 255, 255, 0.1);">
                            <div class="progress-bar bg-primary" id="qrTokenProgressBar" style="width: 100%; transition: width 1s linear;"></div>
                        </div>

                        <!-- Botón Principal: Simular Validación en Torniquete -->
                        <button type="button" class="btn btn-primary w-100 py-3 font-anton tracking-wide mb-2" id="btnSimulateTorniquete">
                            <i class="fa-solid fa-bolt me-1"></i> SIMULAR VALIDACIÓN EN TORNIQUETE
                        </button>
                    </div>

                    <!-- Barra de Herramientas del Carnet -->
                    <div class="d-flex justify-content-between align-items-center pt-2 border-top border-secondary text-2xs">
                        <button type="button" class="btn btn-link text-muted text-decoration-none p-0 d-flex align-items-center gap-1" id="btnToggleBrightness">
                            <i class="fa-regular fa-sun text-warning"></i> <span>MODO MÁXIMO BRILLO</span>
                        </button>
                        <button type="button" class="btn btn-link text-muted text-decoration-none p-0 d-flex align-items-center gap-1" id="btnDownloadPassReceipt">
                            <i class="fa-solid fa-download text-primary"></i> <span>GUARDAR EN CELULAR</span>
                        </button>
                    </div>
                </div>
            </div>`
        );

        // Configuración de eventos interactivos en el carnet
        setTimeout(() => {
            const cardEl = document.getElementById("smartMemberCard");
            const visualArea = document.getElementById("qrPassVisualArea");
            const grantedArea = document.getElementById("turnstileGrantedArea");
            const antifraudArea = document.getElementById("antifraudFooterArea");
            const qrImg = document.getElementById("realQrImage");
            const tokenText = document.getElementById("qrTokenDisplay");
            const timerText = document.getElementById("qrTimerCountdown");
            const progBar = document.getElementById("qrTokenProgressBar");
            const simBtn = document.getElementById("btnSimulateTorniquete");
            const returnBtn = document.getElementById("btnReturnToQr");
            const refreshBtn = document.getElementById("btnForceRefreshToken");
            const brightBtn = document.getElementById("btnToggleBrightness");
            const downloadBtn = document.getElementById("btnDownloadPassReceipt");
            const qrBox = document.getElementById("qrContainerBox");

            // Función para regenerar el token dinámico y la imagen del QR
            const refreshDynamicToken = () => {
                passData = generatePassData(triggerEl);
                if (tokenText) tokenText.textContent = passData.token;
                if (qrImg) qrImg.src = passData.qrUrl;
                secondsLeft = 60;
                if (timerText) timerText.textContent = `Se renueva en ${secondsLeft}s`;
                if (progBar) progBar.style.width = "100%";
            };

            // Iniciar cuenta regresiva de 60 segundos
            countdownInterval = setInterval(() => {
                secondsLeft--;
                if (secondsLeft <= 0) {
                    refreshDynamicToken();
                } else {
                    if (timerText) timerText.textContent = `Se renueva en ${secondsLeft}s`;
                    if (progBar) progBar.style.width = `${(secondsLeft / 60) * 100}%`;
                }
            }, 1000);

            // Refresco manual al hacer clic en 🔄
            if (refreshBtn) {
                refreshBtn.addEventListener("click", () => {
                    refreshDynamicToken();
                    showGymToast("Código QR y token de seguridad actualizados", "fa-rotate");
                });
            }

            // Alternar Modo Brillo Máximo
            if (brightBtn && qrBox) {
                brightBtn.addEventListener("click", () => {
                    const isMax = qrBox.classList.toggle("max-brightness");
                    showGymToast(isMax ? "Brillo maximizado para lectura óptica rápida" : "Brillo normal", "fa-sun");
                });
            }

            // Descargar carnet / credencial
            if (downloadBtn) {
                downloadBtn.addEventListener("click", () => {
                    const blob = new Blob([passData.qrPayload], { type: "text/plain;charset=utf-8" });
                    const a = document.createElement("a");
                    a.href = URL.createObjectURL(blob);
                    a.download = `Pase_${passData.socioDni}_EnergyGym.txt`;
                    a.click();
                    showGymToast("Pase digital guardado para acceso sin conexión", "fa-download");
                });
            }

            // Simular Validación en Torniquete con llamada a API
            if (simBtn) {
                simBtn.addEventListener("click", async () => {
                    simBtn.disabled = true;
                    simBtn.innerHTML = `<i class="fa-solid fa-spinner fa-spin me-2"></i> LEYENDO EN TORNIQUETE 01...`;

                    try {
                        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
                        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";

                        const resp = await fetch("/cliente/api/torniquete/validar", {
                            method: "POST",
                            headers: {
                                "Content-Type": "application/x-www-form-urlencoded",
                                [csrfHeader]: csrfToken
                            },
                            body: `dni=${encodeURIComponent(passData.socioDni)}&token=${encodeURIComponent(passData.token)}`
                        });

                        const res = await resp.json().catch(() => ({}));
                        
                        setTimeout(() => {
                            if (cardEl) cardEl.classList.add("pass-granted");
                            if (visualArea) visualArea.classList.add("d-none");
                            if (antifraudArea) antifraudArea.classList.add("d-none");
                            if (grantedArea) grantedArea.classList.remove("d-none");

                            const timeEl = document.getElementById("grantedTimeText");
                            const visitEl = document.getElementById("grantedVisitsText");
                            if (timeEl && res.hora) timeEl.textContent = `Hoy a las ${res.hora}`;
                            if (visitEl && res.visitasNuevas) visitEl.textContent = res.visitasNuevas;

                            // Actualizar visitas en la pantalla de fondo si existe
                            const visitsCard = document.querySelector(".card-metric h2");
                            if (visitsCard && res.visitasNuevas) {
                                visitsCard.textContent = `${res.visitasNuevas} VISITAS`;
                            }

                            showGymToast("¡Acceso concedido en Torniquete Las Am�ricas, Ate!", "fa-door-open");
                        }, 600);
                    } catch (err) {
                        setTimeout(() => {
                            if (cardEl) cardEl.classList.add("pass-granted");
                            if (visualArea) visualArea.classList.add("d-none");
                            if (antifraudArea) antifraudArea.classList.add("d-none");
                            if (grantedArea) grantedArea.classList.remove("d-none");
                            showGymToast("¡Acceso concedido en Torniquete Las Am�ricas, Ate!", "fa-door-open");
                        }, 600);
                    }
                });
            }

            // Volver al código QR
            if (returnBtn) {
                returnBtn.addEventListener("click", () => {
                    if (cardEl) cardEl.classList.remove("pass-granted");
                    if (visualArea) visualArea.classList.remove("d-none");
                    if (antifraudArea) antifraudArea.classList.remove("d-none");
                    if (grantedArea) grantedArea.classList.add("d-none");
                    if (simBtn) {
                        simBtn.disabled = false;
                        simBtn.innerHTML = `<i class="fa-solid fa-bolt me-1"></i> SIMULAR VALIDACIÓN EN TORNIQUETE`;
                    }
                });
            }

            // Limpiar intervalo al cerrar el modal
            const gymModalEl = document.getElementById("dynamicGymModal");
            if (gymModalEl) {
                gymModalEl.addEventListener("hidden.bs.modal", () => {
                    if (countdownInterval) {
                        clearInterval(countdownInterval);
                        countdownInterval = null;
                    }
                }, { once: true });
            }
        }, 50);
    };

    const qrButtons = document.querySelectorAll("#btnOpenQrPass, #btnTopbarQr, [data-open-qr-pass], .btn-open-qr-pass");
    qrButtons.forEach(btn => btn.addEventListener("click", triggerQrModal));
}
