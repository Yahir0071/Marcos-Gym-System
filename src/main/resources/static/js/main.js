// main.js - Energy Gym Interactive Scripts

document.addEventListener("DOMContentLoaded", function() {
    console.log("Energy Gym Loaded!");

    // Initialize Bootstrap tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    });

    // Handle interactive buttons (e.g. reserving a class)
    const reserveButtons = document.querySelectorAll('.btn-reserve');
    reserveButtons.forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            // Show a simple alert for now
            alert("¡Reserva confirmada!");
        });
    });

    // Form validation example for login/registration
    const forms = document.querySelectorAll('.needs-validation')
    Array.prototype.slice.call(forms).forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) {
                event.preventDefault()
                event.stopPropagation()
            }
            form.classList.add('was-validated')
        }, false)
    });
});
