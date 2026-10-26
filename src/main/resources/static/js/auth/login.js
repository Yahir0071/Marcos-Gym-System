/**
 * auth/login.js - Energy Gym Authentication Interactions
 * Responsabilidad: alternancia de visibilidad de contraseña en el formulario de acceso.
 */

export function initLoginControls() {
    const togglePassBtn = document.getElementById("togglePasswordBtn");

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
