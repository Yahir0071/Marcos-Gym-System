/**
 * auth/login.js - Energy Gym Authentication Interactions
 * Responsabilidad: Acceso rápido con credenciales demo y alternancia de visibilidad de contraseña.
 */

export function initLoginControls() {
    const btnQuickClient = document.getElementById("btnQuickClientLogin");
    const btnQuickAdmin = document.getElementById("btnQuickAdminLogin");
    const loginForm = document.getElementById("loginForm");
    const togglePassBtn = document.getElementById("togglePasswordBtn");

    if (btnQuickClient && loginForm) {
        btnQuickClient.addEventListener("click", () => {
            const userInput = document.getElementById("username");
            const passInput = document.getElementById("password");
            const roleInput = document.getElementById("loginRoleInput");
            if (userInput) userInput.value = "cliente@energygym.pe";
            if (passInput) passInput.value = "cliente123";
            if (roleInput) roleInput.value = "cliente";
            loginForm.submit();
        });
    }

    if (btnQuickAdmin && loginForm) {
        btnQuickAdmin.addEventListener("click", () => {
            const userInput = document.getElementById("username");
            const passInput = document.getElementById("password");
            const roleInput = document.getElementById("loginRoleInput");
            if (userInput) userInput.value = "admin@energygym.pe";
            if (passInput) passInput.value = "admin123";
            if (roleInput) roleInput.value = "admin";
            loginForm.submit();
        });
    }

    if (togglePassBtn) {
        togglePassBtn.addEventListener("click", function () {
            const passInput = document.getElementById("password");
            const icon = this.querySelector("i");
            if (!passInput) return;

            if (passInput.type === "password") {
                passInput.type = "text";
                if (icon) icon.className = "fa-regular fa-eye-slash";
            } else {
                passInput.type = "password";
                if (icon) icon.className = "fa-regular fa-eye";
            }
        });
    }
}
