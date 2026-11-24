/**
 * auth/login.js - Energy Gym Authentication Interactions
 * Responsabilidad: Control accesible de visibilidad de contraseña y validación básica de formulario.
 */

export function initLoginControls() {
    const togglePassBtn = document.getElementById("togglePasswordBtn");

    if (togglePassBtn) {
        togglePassBtn.setAttribute("aria-pressed", "false");
        togglePassBtn.setAttribute("aria-label", "Mostrar contraseña");

        togglePassBtn.addEventListener("click", function () {
            const passInput = document.getElementById("password");
            const icon = this.querySelector("i");
            if (!passInput) return;

            if (passInput.type === "password") {
                passInput.type = "text";
                this.setAttribute("aria-pressed", "true");
                this.setAttribute("aria-label", "Ocultar contraseña");
                if (icon) icon.className = "fa-regular fa-eye-slash";
            } else {
                passInput.type = "password";
                this.setAttribute("aria-pressed", "false");
                this.setAttribute("aria-label", "Mostrar contraseña");
                if (icon) icon.className = "fa-regular fa-eye";
            }
        });
    }
}
