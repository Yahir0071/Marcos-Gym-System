import os

base_dir = "/Users/kennedysoto/EnergyGym/src/main/resources"
css_file = os.path.join(base_dir, "static/css/style.css")

css_content = """@import url('https://fonts.googleapis.com/css2?family=Anton&family=Inter:wght@400;600;800&display=swap');

:root {
    --primary: #d8ff00;
    --primary-hover: #bde000;
    --bg-dark: #0a0b0e;
    --bg-card: #11151c;
    --bg-surface: rgba(255,255,255,0.03);
    --text-main: #ffffff;
    --text-muted: #8892a0;
    --border-color: #1e2634;
    --sidebar-width: 260px;
    --navbar-height: 90px;
}

/* ================== GLOBAL ================== */
body {
    background-color: var(--bg-dark);
    color: var(--text-main);
    font-family: var(--font-body);
    -webkit-font-smoothing: antialiased;
}

/* ================== TYPOGRAPHY ================== */
.font-anton, h1, h2, h3, h4, h5, h6, .display-1, .display-2, .display-3, .display-4 {
    font-family: var(--font-display);
    letter-spacing: 1px;
    text-transform: uppercase;
    font-weight: normal;
}

.text-primary { color: var(--primary) !important; }
.text-muted { color: var(--text-muted) !important; }
.bg-primary { background-color: var(--primary) !important; color: #000 !important; }

.tracking-wide { letter-spacing: 1px; }
.tracking-wider { letter-spacing: 2px; }
.leading-none { line-height: 1 !important; }
.text-2xs { font-size: 0.65rem; }
.text-xs { font-size: 0.75rem; }
.text-sm { font-size: 0.85rem; }
.text-md { font-size: 1rem; }
.text-lg { font-size: 1.1rem; }
.text-xl { font-size: 1.5rem; }
.text-3xl { font-size: 3rem; }
.text-6xl { font-size: 6rem; }

.section-subtitle {
    font-size: 0.75rem;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 2px;
    color: var(--text-muted);
    display: flex;
    align-items: center;
}
.section-subtitle.has-line::before {
    content: "";
    display: inline-block;
    width: 30px;
    height: 3px;
    background-color: var(--primary);
    margin-right: 15px;
}

/* ================== COMPONENTS ================== */
/* Navbar */
.navbar-main {
    background-color: var(--bg-dark);
    border-bottom: 1px solid var(--border-color);
    padding: 1.5rem 0;
}
.navbar-brand {
    font-size: 1.6rem;
    letter-spacing: 1px;
}
.nav-link {
    font-size: 0.8rem;
    font-weight: 800;
    text-transform: uppercase;
    color: var(--text-main) !important;
    padding: 0.5rem 1.2rem !important;
    letter-spacing: 0.5px;
    transition: color 0.3s;
}
.nav-link.active, .nav-link:hover { color: var(--primary) !important; }

/* Buttons */
.btn {
    border-radius: 0;
    font-weight: 800;
    text-transform: uppercase;
    padding: 12px 24px;
    font-size: 0.85rem;
    letter-spacing: 0.5px;
    transition: all 0.3s ease;
}
.btn-primary { background-color: var(--primary); color: #000; border: none; }
.btn-primary:hover { background-color: var(--primary-hover); color: #000; }
.btn-outline { background-color: var(--bg-card); color: var(--text-main); border: 1px solid var(--border-color); }
.btn-outline:hover { border-color: var(--primary); color: var(--text-main); }
.btn-link-white { color: var(--text-main); text-decoration: none; }
.btn-link-white:hover { color: var(--primary); }

/* Cards */
.card { background-color: var(--bg-card); border: 1px solid var(--border-color); border-radius: 0; color: var(--text-main); }
.card-active { border-color: var(--primary); }

/* Forms */
.form-label { font-size: 0.75rem; text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px; color: var(--text-muted); }
.form-control, .form-select {
    background-color: var(--bg-card) !important;
    border: 1px solid var(--border-color) !important;
    border-radius: 0;
    color: var(--text-main) !important;
    padding: 12px;
    font-size: 0.9rem;
}
.form-control:focus, .form-select:focus { border-color: var(--primary) !important; box-shadow: none; }
.form-control::placeholder { color: var(--text-muted); }

/* Tables */
.table-custom { color: var(--text-main); vertical-align: middle; }
.table-custom th { font-family: var(--font-display); color: var(--text-muted); font-size: 0.8rem; letter-spacing: 1px; border-bottom: 1px solid var(--border-color); padding: 1rem; }
.table-custom td { padding: 1rem; border-bottom: 1px solid var(--border-color); font-size: 0.85rem; }

/* Progress Bars */
.progress-custom { height: 15px; background-color: var(--bg-surface); border-radius: 0; }
.progress-bar-custom { background-color: var(--primary); }

/* ================== LAYOUT GRIDS & SECTIONS ================== */
.page-container { padding-top: var(--navbar-height); min-height: 100vh; padding-bottom: 5rem; }
.hero-container { padding-top: var(--navbar-height); min-height: 100vh; display: flex; flex-direction: column; justify-content: center; padding-bottom: 5rem; }

/* CSS Grids */
.grid-features { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1rem; }
.grid-plans { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; }
.grid-products { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 1.5rem; }
.grid-dashboard { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; }
.grid-2-col { display: grid; grid-template-columns: 1fr 1fr; gap: 2rem; }

/* Client Layout */
.client-wrapper { display: flex; min-height: 100vh; }
.client-sidebar { width: var(--sidebar-width); height: 100vh; background-color: var(--bg-dark); border-right: 1px solid var(--border-color); position: fixed; left: 0; top: 0; z-index: 1000; display: flex; flex-direction: column; }
.client-main { margin-left: var(--sidebar-width); flex-grow: 1; display: flex; flex-direction: column; }
.client-topbar { padding: 1.5rem 2.5rem; display: flex; justify-content: flex-end; align-items: center; background-color: var(--bg-dark); }
.client-content { padding: 2.5rem; flex-grow: 1; }

.sidebar-link {
    font-weight: 800; font-size: 0.75rem; text-transform: uppercase;
    color: var(--text-muted); padding: 0.75rem 1.5rem; transition: background-color 0.2s;
    text-decoration: none; display: block;
}
.sidebar-link.active { background-color: var(--primary); color: var(--bg-dark); }
.sidebar-link:hover:not(.active) { color: var(--text-main); }

/* ================== UTILITIES & DECORATIONS ================== */
.decor-bar-vertical { width: 8px; height: 350px; background-color: var(--primary); position: absolute; right: 40px; top: 50px; }
.decor-logo-box { background-color: var(--bg-card); min-height: 450px; position: relative; display: flex; align-items: center; justify-content: center; border: 1px solid var(--border-color); }
.decor-glass { background-color: rgba(255,255,255,0.1); padding: 40px 60px; backdrop-filter: blur(5px); z-index: 2; border-radius: 8px; }
.decor-shadow-box { background-color: #000; width: 200px; height: 250px; position: absolute; z-index: 1; }

.promo-banner { background: linear-gradient(90deg, rgba(216,255,0,0.1) 0%, rgba(17,21,28,1) 100%); position: relative; overflow: hidden; border: none; }
.product-image-box { background-color: var(--bg-surface); height: 200px; display: flex; justify-content: center; align-items: center; }
.product-detail-box { height: 500px; background-color: var(--bg-surface); display: flex; flex-direction: column; justify-content: center; align-items: center; }
.product-jar { width: 160px; height: 260px; background-color: #000; border-radius: 10px 10px 0 0; border-top-left-radius: 30px; border-top-right-radius: 30px; position: relative; display: flex; align-items: center; justify-content: center; border: 1px solid #333; }
.product-jar-lid { width: 100px; height: 20px; background-color: #444; border-radius: 5px; position: absolute; top: -20px; }
.thumbnail-box { width: 80px; height: 60px; cursor: pointer; background-color: var(--bg-surface); display: flex; justify-content: center; align-items: center; border: 1px solid var(--border-color); }
.thumbnail-box.active { border-bottom: 2px solid var(--primary) !important; background-color: transparent; }

.login-split-left { background-color: var(--bg-card); position: relative; }
.login-split-divider { width: 6px; height: 300px; background-color: var(--primary); position: absolute; right: 0; top: 50%; transform: translateY(-50%); z-index: 1; }
.notification-line { width: 4px; height: 100%; background-color: var(--primary); position: absolute; left: 0; top: 0; }
"""

os.makedirs(os.path.dirname(css_file), exist_ok=True)
with open(css_file, "w") as f:
    f.write(css_content)

templates = {
    "fragments/navbar.html": """<nav th:fragment="navbar" class="navbar navbar-expand-lg navbar-main fixed-top">
    <div class="container-fluid px-5">
        <a class="navbar-brand text-light d-flex align-items-center font-anton" th:href="@{/inicio}">
            <span class="text-primary me-2">⚡</span> ENERGY GYM
        </a>
        <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse justify-content-end" id="navbarNav">
            <ul class="navbar-nav gap-3">
                <li class="nav-item"><a class="nav-link active" th:href="@{/inicio}">INICIO</a></li>
                <li class="nav-item"><a class="nav-link" th:href="@{/planes}">PLANES</a></li>
                <li class="nav-item"><a class="nav-link" th:href="@{/promociones}">PROMOCIONES</a></li>
                <li class="nav-item"><a class="nav-link" th:href="@{/productos}">PRODUCTOS</a></li>
                <li class="nav-item"><a class="nav-link" th:href="@{/afluencia}">AFLUENCIA</a></li>
                <li class="nav-item ms-4"><a class="nav-link text-light" th:href="@{/login}">INICIAR SESIÓN</a></li>
            </ul>
        </div>
    </div>
</nav>""",

    "fragments/cliente-sidebar.html": """<div th:fragment="sidebar(activeItem)" class="client-sidebar">
    <div class="p-4 mb-3">
        <a class="text-light text-decoration-none d-flex align-items-center" th:href="@{/inicio}">
            <span class="text-primary me-2">⚡</span> <span class="font-anton text-lg tracking-wide">ENERGY GYM</span>
        </a>
    </div>
    <div class="flex-grow-1 d-flex flex-column gap-2 px-3 mt-4">
        <a th:href="@{/cliente/mi-membresia}" th:class="'sidebar-link ' + (${activeItem == 'membresia'} ? 'active' : '')">MI MEMBRESÍA</a>
        <a th:href="@{/cliente/productos}" th:class="'sidebar-link ' + (${activeItem == 'productos'} ? 'active' : '')">PRODUCTOS</a>
        <a th:href="@{/cliente/mis-reservas}" th:class="'sidebar-link ' + (${activeItem == 'reservas'} ? 'active' : '')">MIS RESERVAS</a>
        <a th:href="@{/cliente/afluencia}" th:class="'sidebar-link ' + (${activeItem == 'afluencia'} ? 'active' : '')">AFLUENCIA</a>
        <a th:href="@{/cliente/notificaciones}" th:class="'sidebar-link ' + (${activeItem == 'notificaciones'} ? 'active' : '')">NOTIFICACIONES</a>
    </div>
    <div class="p-4 mt-auto">
        <a th:href="@{/logout}" class="sidebar-link">CERRAR SESIÓN</a>
    </div>
</div>""",

    "fragments/cliente-topbar.html": """<div th:fragment="topbar" class="client-topbar w-100">
    <div class="d-flex align-items-center gap-2">
        <div class="bg-primary rounded-circle" style="width:8px; height:8px;"></div>
        <span class="text-uppercase font-anton text-light text-sm tracking-wide">CARLOS MENDOZA</span>
    </div>
</div>""",

    "public/inicio.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Inicio - Energy Gym</title></head>
<body>
    <section>
        <div th:replace="~{fragments/navbar :: navbar}"></div>
        <div class="container-fluid px-5 hero-container">
            <div class="row align-items-center mb-5">
                <div class="col-lg-7 pe-lg-5">
                    <div class="section-subtitle has-line mb-4">TU MOMENTO ES AHORA</div>
                    <h1 class="display-1 font-anton leading-none mb-3 text-6xl">
                        TU MEJOR VERSIÓN<br><span class="text-primary">COMIENZA AQUÍ</span>
                    </h1>
                    <p class="fs-5 mb-5 mt-4 tracking-wider">Entrena &nbsp;&bull;&nbsp; Cuida tu salud &nbsp;&bull;&nbsp; Cumple tus metas</p>
                    <div class="d-flex gap-3">
                        <a th:href="@{/planes}" class="btn btn-primary d-flex align-items-center gap-2">VER PLANES <i class="fa-solid fa-arrow-right"></i></a>
                        <a href="#" class="btn btn-outline">CONOCE MÁS</a>
                    </div>
                </div>
                <div class="col-lg-5 position-relative mt-5 mt-lg-0">
                    <div class="decor-logo-box">
                        <div class="decor-bar-vertical"></div>
                        <div class="decor-glass">
                            <h2 class="font-anton text-center m-0 text-3xl leading-none">
                                <span class="text-light">ENERGY</span><br><span class="text-primary">GYM</span>
                            </h2>
                        </div>
                        <div class="decor-shadow-box"></div>
                    </div>
                </div>
            </div>
            <div class="grid-features mt-5 pt-4">
                <div class="card p-4 d-flex flex-row align-items-center gap-3">
                    <span class="text-primary font-anton fs-3">01</span>
                    <h6 class="font-anton m-0 tracking-wide text-lg">MODERNAS INSTALACIONES</h6>
                </div>
                <div class="card p-4 d-flex flex-row align-items-center gap-3">
                    <span class="text-primary font-anton fs-3">02</span>
                    <h6 class="font-anton m-0 tracking-wide text-lg">EQUIPOS DE CALIDAD</h6>
                </div>
                <div class="card p-4 d-flex flex-row align-items-center gap-3">
                    <span class="text-primary font-anton fs-3">03</span>
                    <h6 class="font-anton m-0 tracking-wide text-lg">AMBIENTE MOTIVADOR</h6>
                </div>
                <div class="card p-4 d-flex flex-row align-items-center gap-3">
                    <span class="text-primary font-anton fs-3">04</span>
                    <h6 class="font-anton m-0 tracking-wide text-lg">RESULTADOS REALES</h6>
                </div>
            </div>
        </div>
    </section>
</body>
</html>""",

    "public/planes.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Planes - Energy Gym</title></head>
<body>
    <section>
        <div th:replace="~{fragments/navbar :: navbar}"></div>
        <div class="container-fluid px-5 page-container">
            <div class="section-subtitle has-line mb-3">ELIGE TU PLAN</div>
            <h1 class="display-3 font-anton leading-none mb-1 text-light">PLANES DE<br><span class="text-primary">MEMBRESÍA</span></h1>
            <p class="font-anton text-muted mb-5 tracking-wide text-lg">ENTRENA A TU MANERA</p>

            <div class="grid-plans mb-5">
                <div class="card p-4 d-flex flex-column">
                    <h3 class="font-anton mb-2">BÁSICO</h3>
                    <p class="text-muted text-sm mb-4">Ideal para empezar</p>
                    <div class="d-flex align-items-baseline gap-2 mb-4">
                        <h2 class="font-anton m-0 text-light text-3xl">S/ 79</h2>
                        <span class="text-muted text-xs font-anton">AL MES</span>
                    </div>
                    <ul class="list-unstyled mb-5">
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Acceso a todas las áreas</li>
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Equipos de última generación</li>
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Horario completo</li>
                    </ul>
                    <button class="btn btn-outline mt-auto w-100 text-start">ELEGIR PLAN</button>
                </div>
                <div class="card card-active p-4 d-flex flex-column">
                    <h3 class="font-anton mb-2">PLUS</h3>
                    <p class="text-muted text-sm mb-4">El más elegido</p>
                    <div class="d-flex align-items-baseline gap-2 mb-4">
                        <h2 class="font-anton m-0 text-light text-3xl">S/ 119</h2>
                        <span class="text-muted text-xs font-anton">AL MES</span>
                    </div>
                    <ul class="list-unstyled mb-5">
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Acceso a todas las áreas</li>
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Equipos de última generación</li>
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Horario completo</li>
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Acceso a zona premium</li>
                    </ul>
                    <button class="btn btn-primary mt-auto w-100 text-start text-dark">ELEGIR PLAN</button>
                </div>
                <div class="card p-4 d-flex flex-column">
                    <h3 class="font-anton mb-2">PREMIUM</h3>
                    <p class="text-muted text-sm mb-4">Máximo rendimiento</p>
                    <div class="d-flex align-items-baseline gap-2 mb-4">
                        <h2 class="font-anton m-0 text-light text-3xl">S/ 159</h2>
                        <span class="text-muted text-xs font-anton">AL MES</span>
                    </div>
                    <ul class="list-unstyled mb-5">
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Acceso a todas las áreas</li>
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Equipos de última generación</li>
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Horario completo</li>
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Acceso a zona premium</li>
                        <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Beneficios exclusivos</li>
                    </ul>
                    <button class="btn btn-outline mt-auto w-100 text-start">ELEGIR PLAN</button>
                </div>
            </div>
            <div class="card p-3 d-flex flex-row align-items-center gap-3">
                <i class="fa-solid fa-circle-info text-muted"></i>
                <span class="text-muted font-anton text-xs tracking-wide">LA ACTIVACIÓN DEL PLAN ES PRESENCIAL EN NUESTRO LOCAL.</span>
            </div>
        </div>
    </section>
</body>
</html>""",

    "public/promociones.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Promociones - Energy Gym</title></head>
<body>
    <section>
        <div th:replace="~{fragments/navbar :: navbar}"></div>
        <div class="container-fluid px-5 page-container">
            <div class="section-subtitle has-line mb-3">APROVECHA AHORA</div>
            <h1 class="display-3 font-anton leading-none mb-1 text-light">PROMOCIONES<br><span class="text-primary">EXCLUSIVAS</span></h1>
            <p class="font-anton text-muted mb-5 tracking-wide text-lg">MÁS ENERGÍA, MÁS RESULTADOS.</p>
            <hr class="border-secondary mb-5">
            <div class="card promo-banner p-5 mb-5 d-flex justify-content-between align-items-center flex-row flex-wrap gap-4 z-1">
                <div>
                    <div class="badge bg-primary font-anton mb-3 px-3 py-2 text-xs">PROMOCIÓN DESTACADA</div>
                    <h2 class="font-anton text-light text-3xl leading-none">2 MESES<br>X EL PRECIO DE 1</h2>
                    <p class="text-muted mt-3 mb-0 text-sm">Duplica tu energía este mes. Vigencia: 01-31 Ene 2025.</p>
                </div>
                <button class="btn btn-primary d-flex align-items-center gap-2">VER DETALLE <i class="fa-solid fa-arrow-right"></i></button>
            </div>
            <h5 class="font-anton text-light mb-4">TODAS LAS PROMOCIONES ACTIVAS</h5>
            <div class="grid-plans">
                <div class="card p-4 d-flex flex-column position-relative">
                    <span class="badge bg-primary position-absolute top-0 end-0 m-3 font-anton">ACTIVA</span>
                    <h4 class="font-anton mt-2 mb-3">SHAKER GRATIS</h4>
                    <p class="text-muted text-sm mb-5">En planes Plus o Premium</p>
                    <button class="btn btn-outline w-100 mt-auto text-start">VER DETALLE</button>
                </div>
                <div class="card p-4 d-flex flex-column position-relative">
                    <span class="badge bg-primary position-absolute top-0 end-0 m-3 font-anton">ACTIVA</span>
                    <h4 class="font-anton mt-2 mb-3">20% DSCTO</h4>
                    <p class="text-muted text-sm mb-5">En accesorios seleccionados</p>
                    <button class="btn btn-outline w-100 mt-auto text-start">VER DETALLE</button>
                </div>
                <div class="card p-4 d-flex flex-column position-relative">
                    <span class="badge bg-primary position-absolute top-0 end-0 m-3 font-anton">ACTIVA</span>
                    <h4 class="font-anton mt-2 mb-3">COMBO SUPLEMENTOS</h4>
                    <p class="text-muted text-sm mb-5">Ahorra en packs seleccionados</p>
                    <button class="btn btn-outline w-100 mt-auto text-start">VER DETALLE</button>
                </div>
            </div>
        </div>
    </section>
</body>
</html>""",

    "public/productos.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Productos - Energy Gym</title></head>
<body>
    <section>
        <div th:replace="~{fragments/navbar :: navbar}"></div>
        <div class="container-fluid px-5 page-container">
            <div class="section-subtitle has-line mb-3">NUTRE TU ESFUERZO</div>
            <h1 class="display-3 font-anton leading-none mb-1 text-light">PRODUCTOS<br><span class="text-primary">SUPLEMENTOS Y MÁS</span></h1>
            <p class="font-anton text-muted mb-4 tracking-wide text-lg">CALIDAD PARA TU MEJOR VERSIÓN</p>
            <label class="form-label mt-3">BUSCAR PRODUCTOS</label>
            <input type="text" class="form-control mb-4" placeholder="Buscar productos, marcas o categorías...">
            <div class="d-flex gap-2 flex-wrap mb-5">
                <button class="btn btn-primary px-4">TODOS</button>
                <button class="btn btn-outline px-4">SUPLEMENTOS</button>
                <button class="btn btn-outline px-4">ACCESORIOS</button>
                <button class="btn btn-outline px-4">ROPA</button>
                <button class="btn btn-outline px-4">BOTELLAS</button>
                <button class="btn btn-outline px-4">OTROS</button>
            </div>
            <div class="grid-products mb-5">
                <div class="card d-flex flex-column">
                    <div class="product-image-box">
                        <h4 class="font-anton text-primary m-0">ENERGY</h4>
                    </div>
                    <div class="p-4 d-flex flex-column flex-grow-1">
                        <span class="badge text-success mb-3 align-self-start border border-success bg-transparent tracking-wide text-xs">DISPONIBLE</span>
                        <h6 class="font-anton mb-1">WHEY PROTEIN 2 LB</h6>
                        <h4 class="font-anton text-light mb-4">S/ 149</h4>
                        <button class="btn btn-outline w-100 text-start mt-auto">VER DETALLE</button>
                    </div>
                </div>
                <div class="card d-flex flex-column">
                    <div class="product-image-box">
                        <h4 class="font-anton text-primary m-0">ENERGY</h4>
                    </div>
                    <div class="p-4 d-flex flex-column flex-grow-1">
                        <span class="badge text-success mb-3 align-self-start border border-success bg-transparent tracking-wide text-xs">DISPONIBLE</span>
                        <h6 class="font-anton mb-1">CREATINA 300 G</h6>
                        <h4 class="font-anton text-light mb-4">S/ 89</h4>
                        <button class="btn btn-outline w-100 text-start mt-auto">VER DETALLE</button>
                    </div>
                </div>
                <div class="card d-flex flex-column">
                    <div class="product-image-box">
                        <h4 class="font-anton text-primary m-0">ENERGY</h4>
                    </div>
                    <div class="p-4 d-flex flex-column flex-grow-1">
                        <span class="badge text-success mb-3 align-self-start border border-success bg-transparent tracking-wide text-xs">DISPONIBLE</span>
                        <h6 class="font-anton mb-1">SHAKER 700 ML</h6>
                        <h4 class="font-anton text-light mb-4">S/ 39</h4>
                        <button class="btn btn-outline w-100 text-start mt-auto">VER DETALLE</button>
                    </div>
                </div>
                <div class="card d-flex flex-column">
                    <div class="product-image-box">
                        <h4 class="font-anton text-primary m-0">ENERGY</h4>
                    </div>
                    <div class="p-4 d-flex flex-column flex-grow-1">
                        <span class="badge text-success mb-3 align-self-start border border-success bg-transparent tracking-wide text-xs">DISPONIBLE</span>
                        <h6 class="font-anton mb-1">CORREAS DE LEVANTE</h6>
                        <h4 class="font-anton text-light mb-4">S/ 59</h4>
                        <button class="btn btn-outline w-100 text-start mt-auto">VER DETALLE</button>
                    </div>
                </div>
            </div>
        </div>
    </section>
</body>
</html>""",

    "public/afluencia.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Afluencia - Energy Gym</title></head>
<body>
    <section>
        <div th:replace="~{fragments/navbar :: navbar}"></div>
        <div class="container-fluid px-5 page-container">
            <div class="section-subtitle has-line mb-3">PLANIFICA TU VISITA</div>
            <h1 class="display-3 font-anton leading-none mb-1 text-light">AFLUENCIA<br><span class="text-primary">EN EL GIMNASIO</span></h1>
            <p class="font-anton text-muted mb-5 tracking-wide text-lg">CONOCE LOS HORARIOS CON MAYOR Y MENOR AFLUENCIA</p>
            <div class="card p-0 mb-4">
                <div class="p-4 border-bottom border-secondary d-flex align-items-center flex-wrap gap-4">
                    <div style="width: 150px;">
                        <h6 class="font-anton m-0">MAÑANA</h6>
                        <small class="text-muted">06:00 - 11:00</small>
                    </div>
                    <div class="flex-grow-1">
                        <div class="progress progress-custom">
                            <div class="progress-bar progress-bar-custom" role="progressbar" style="width: 60%;"></div>
                        </div>
                    </div>
                    <div class="text-end" style="width: 150px;">
                        <h4 class="font-anton text-light m-0">60%</h4>
                        <small class="text-muted text-xs">AFLUENCIA MODERADA</small>
                    </div>
                </div>
                <div class="p-4 border-bottom border-secondary d-flex align-items-center flex-wrap gap-4">
                    <div style="width: 150px;">
                        <h6 class="font-anton m-0">MEDIODÍA</h6>
                        <small class="text-muted">11:00 - 18:00</small>
                    </div>
                    <div class="flex-grow-1">
                        <div class="progress progress-custom">
                            <div class="progress-bar progress-bar-custom" role="progressbar" style="width: 85%;"></div>
                        </div>
                    </div>
                    <div class="text-end" style="width: 150px;">
                        <h4 class="font-anton text-light m-0">85%</h4>
                        <small class="text-muted text-xs">AFLUENCIA ALTA</small>
                    </div>
                </div>
                <div class="p-4 d-flex align-items-center flex-wrap gap-4">
                    <div style="width: 150px;">
                        <h6 class="font-anton m-0">TARDE-NOCHE</h6>
                        <small class="text-muted">18:00 - 23:00</small>
                    </div>
                    <div class="flex-grow-1">
                        <div class="progress progress-custom">
                            <div class="progress-bar progress-bar-custom" role="progressbar" style="width: 70%;"></div>
                        </div>
                    </div>
                    <div class="text-end" style="width: 150px;">
                        <h4 class="font-anton text-light m-0">70%</h4>
                        <small class="text-muted text-xs">AFLUENCIA MODERADA</small>
                    </div>
                </div>
            </div>
            <div class="card p-3 d-flex flex-row align-items-center gap-4 mb-5">
                <div class="d-flex align-items-center gap-2">
                    <i class="fa-solid fa-circle-info text-muted"></i>
                    <span class="text-muted font-anton text-xs tracking-wide">INFORMACIÓN APROXIMADA, NO EN TIEMPO REAL.</span>
                </div>
                <div class="text-muted font-anton text-xs tracking-wide">ÚLTIMA ACTUALIZACIÓN: 30 ENE 2025</div>
            </div>
        </div>
    </section>
</body>
</html>""",

    "../login.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Iniciar Sesión - Energy Gym</title></head>
<body>
    <section class="d-flex flex-column min-vh-100">
        <div th:replace="~{fragments/navbar :: navbar}"></div>
        <div class="row g-0 flex-grow-1 page-container pb-0">
            <div class="col-md-6 d-flex flex-column justify-content-center p-5 login-split-left">
                <div class="login-split-divider"></div>
                <div class="w-100 mx-auto" style="max-width: 500px;">
                    <div class="section-subtitle mb-3 text-2xs">BIENVENIDO DE NUEVO</div>
                    <h1 class="display-3 font-anton leading-none mb-3 text-light">TU DISCIPLINA<br><span class="text-primary">CONTINÚA AQUÍ</span></h1>
                    <p class="font-anton text-muted tracking-wide text-md">INICIA SESIÓN PARA SEGUIR ENTRENANDO</p>
                </div>
            </div>
            <div class="col-md-6 d-flex flex-column justify-content-center p-5">
                <div class="w-100 mx-auto" style="max-width: 400px;">
                    <h2 class="font-anton mb-4 text-light">INICIAR SESIÓN</h2>
                    <form action="#" th:action="@{/login}" method="post">
                        <div class="mb-4">
                            <label for="username" class="form-label">CORREO ELECTRÓNICO</label>
                            <input type="email" class="form-control" id="username" name="username" placeholder="tu.correo@ejemplo.com" required>
                        </div>
                        <div class="mb-4">
                            <label for="password" class="form-label">CONTRASEÑA</label>
                            <input type="password" class="form-control" id="password" name="password" placeholder="••••••••" required>
                        </div>
                        <button type="submit" class="btn btn-primary w-100 d-flex justify-content-between align-items-center mb-4">
                            INGRESAR <i class="fa-solid fa-arrow-right"></i>
                        </button>
                        <div class="text-center">
                            <a th:href="@{/inicio}" class="btn-link-white font-anton text-xs tracking-wide d-inline-flex align-items-center gap-2">
                                <i class="fa-solid fa-arrow-left"></i> VOLVER AL INICIO
                            </a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </section>
</body>
</html>""",

    "cliente/mi-membresia.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Mi Membresía - Energy Gym</title></head>
<body>
    <section class="client-wrapper">
        <div th:replace="~{fragments/cliente-sidebar :: sidebar('membresia')}"></div>
        <div class="client-main">
            <div th:replace="~{fragments/cliente-topbar :: topbar}"></div>
            <div class="client-content">
                <div class="section-subtitle mb-3 text-primary text-2xs">TU PROGRESO CONTINÚA</div>
                <h1 class="display-3 font-anton leading-none mb-1 text-light">PLAN <span class="text-primary">PLUS</span></h1>
                <p class="font-anton text-light mb-3 tracking-wide text-md">ENTRENA MÁS, CONSIGUE MÁS.</p>
                <span class="badge bg-success font-anton px-3 py-1 mb-5 rounded-0 text-xs tracking-wide">VIGENTE</span>
                
                <div class="grid-dashboard mb-5">
                    <div class="card p-4">
                        <span class="text-muted font-anton mb-2 text-xs tracking-wide">FECHA DE INICIO</span>
                        <h4 class="font-anton m-0">01 Ene 2025</h4>
                    </div>
                    <div class="card p-4">
                        <span class="text-muted font-anton mb-2 text-xs tracking-wide">FECHA DE VENCIMIENTO</span>
                        <h4 class="font-anton m-0">31 Ene 2025</h4>
                    </div>
                    <div class="card p-4">
                        <span class="text-muted font-anton mb-2 text-xs tracking-wide">DÍAS RESTANTES</span>
                        <h4 class="font-anton m-0">15 días</h4>
                    </div>
                </div>
                
                <div class="grid-2-col">
                    <div class="card p-4">
                        <h6 class="font-anton mb-4">BENEFICIOS DE TU PLAN</h6>
                        <ul class="list-unstyled m-0">
                            <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Acceso a todas las áreas</li>
                            <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Equipos de última generación</li>
                            <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Horario completo</li>
                            <li class="mb-3 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Acceso a zona premium</li>
                            <li class="mb-0 text-muted text-sm"><i class="fa-solid fa-check text-light me-2"></i> Beneficios exclusivos</li>
                        </ul>
                    </div>
                    <div class="card p-4 card-active d-flex flex-column">
                        <div class="d-flex align-items-center gap-2 mb-3">
                            <i class="fa-solid fa-triangle-exclamation text-primary"></i>
                            <h6 class="font-anton text-primary m-0">TU PLAN ESTÁ POR VENCER</h6>
                        </div>
                        <p class="text-muted text-sm mb-4">Te quedan 15 días de membresía. Renueva tu plan para que tu progreso no se detenga.</p>
                        <button class="btn btn-primary mt-auto text-start d-flex justify-content-between align-items-center w-100">RENOVAR PLAN <i class="fa-solid fa-arrow-right"></i></button>
                    </div>
                </div>
            </div>
        </div>
    </section>
</body>
</html>""",

    "cliente/mis-reservas.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Mis Reservas - Energy Gym</title></head>
<body>
    <section class="client-wrapper">
        <div th:replace="~{fragments/cliente-sidebar :: sidebar('reservas')}"></div>
        <div class="client-main">
            <div th:replace="~{fragments/cliente-topbar :: topbar}"></div>
            <div class="client-content">
                <div class="section-subtitle mb-3 text-primary text-2xs">TODAS TUS RESERVAS</div>
                <h1 class="display-3 font-anton leading-none mb-1 text-light">MIS RESERVAS</h1>
                <p class="font-anton text-muted mb-5 tracking-wide text-lg">REVISA, SIGUE Y RECOGE TUS PRODUCTOS</p>
                
                <label class="form-label mb-2">BUSCAR RESERVA</label>
                <div class="d-flex gap-3 mb-4">
                    <input type="text" class="form-control w-50" placeholder="Buscar por producto o código...">
                    <select class="form-select w-25">
                        <option>TODOS LOS ESTADOS</option>
                        <option>PENDIENTE</option>
                        <option>CONFIRMADA</option>
                        <option>ENTREGADA</option>
                    </select>
                </div>
                
                <div class="card p-0 overflow-hidden">
                    <div class="table-responsive">
                        <table class="table table-custom table-borderless m-0">
                            <thead>
                                <tr>
                                    <th>CÓDIGO</th><th>FECHA</th><th>PRODUCTO</th><th>CANT.</th><th>ESTADO</th><th>ACCIÓN</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td class="font-anton text-light">#RES-00488</td>
                                    <td class="text-muted">20 Ene 2025</td>
                                    <td class="text-light">Whey Protein 2 LB</td>
                                    <td class="text-light">1</td>
                                    <td><span class="text-muted">PENDIENTE</span></td>
                                    <td><a href="#" class="btn-link-white text-sm">VER DETALLE</a></td>
                                </tr>
                                <tr>
                                    <td class="font-anton text-light">#RES-00487</td>
                                    <td class="text-muted">18 Ene 2025</td>
                                    <td class="text-light">Creatina 300 g</td>
                                    <td class="text-light">2</td>
                                    <td><span class="text-light">CONFIRMADA</span></td>
                                    <td><a href="#" class="btn-link-white text-sm">VER DETALLE</a></td>
                                </tr>
                                <tr>
                                    <td class="font-anton text-light">#RES-00485</td>
                                    <td class="text-muted">15 Ene 2025</td>
                                    <td class="text-light">Shaker 700 ml</td>
                                    <td class="text-light">1</td>
                                    <td><span class="text-light">ENTREGADA</span></td>
                                    <td><a href="#" class="btn-link-white text-sm">VER DETALLE</a></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </section>
</body>
</html>""",

    "cliente/afluencia.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Afluencia - Energy Gym</title></head>
<body>
    <section class="client-wrapper">
        <div th:replace="~{fragments/cliente-sidebar :: sidebar('afluencia')}"></div>
        <div class="client-main">
            <div th:replace="~{fragments/cliente-topbar :: topbar}"></div>
            <div class="client-content">
                <div class="section-subtitle mb-3 text-primary text-2xs">PLANIFICA TU VISITA</div>
                <h1 class="display-3 font-anton leading-none mb-1 text-light">AFLUENCIA EN EL GIMNASIO</h1>
                <p class="font-anton text-muted mb-5 tracking-wide text-lg">CONOCE LOS HORARIOS CON MAYOR Y MENOR AFLUENCIA</p>
                
                <div class="card p-0 mb-4">
                    <div class="p-4 border-bottom border-secondary d-flex align-items-center flex-wrap gap-4">
                        <div style="width: 150px;">
                            <h5 class="font-anton m-0">MAÑANA</h5>
                            <small class="text-muted">06:00 - 11:00</small>
                        </div>
                        <div class="flex-grow-1">
                            <div class="progress progress-custom">
                                <div class="progress-bar progress-bar-custom" role="progressbar" style="width: 60%;"></div>
                            </div>
                        </div>
                        <div class="text-end" style="width: 150px;">
                            <h4 class="font-anton text-light m-0">60%</h4>
                            <small class="text-muted text-xs">AFLUENCIA MODERADA</small>
                        </div>
                    </div>
                    <div class="p-4 border-bottom border-secondary d-flex align-items-center flex-wrap gap-4">
                        <div style="width: 150px;">
                            <h5 class="font-anton m-0">MEDIODÍA</h5>
                            <small class="text-muted">11:00 - 16:00</small>
                        </div>
                        <div class="flex-grow-1">
                            <div class="progress progress-custom">
                                <div class="progress-bar progress-bar-custom" role="progressbar" style="width: 85%;"></div>
                            </div>
                        </div>
                        <div class="text-end" style="width: 150px;">
                            <h4 class="font-anton text-light m-0">85%</h4>
                            <small class="text-muted text-xs">AFLUENCIA ALTA</small>
                        </div>
                    </div>
                    <div class="p-4 d-flex align-items-center flex-wrap gap-4">
                        <div style="width: 150px;">
                            <h5 class="font-anton m-0">TARDE-NOCHE</h5>
                            <small class="text-muted">16:00 - 23:00</small>
                        </div>
                        <div class="flex-grow-1">
                            <div class="progress progress-custom">
                                <div class="progress-bar progress-bar-custom" role="progressbar" style="width: 70%;"></div>
                            </div>
                        </div>
                        <div class="text-end" style="width: 150px;">
                            <h4 class="font-anton text-light m-0">70%</h4>
                            <small class="text-muted text-xs">AFLUENCIA MODERADA</small>
                        </div>
                    </div>
                </div>
                <div class="card p-3 bg-transparent d-flex flex-row align-items-center gap-4 border-secondary mb-5">
                    <div class="d-flex align-items-center gap-2">
                        <i class="fa-solid fa-circle-info text-muted"></i>
                        <span class="text-muted font-anton text-xs tracking-wide">INFORMACIÓN APROXIMADA, NO EN TIEMPO REAL.</span>
                    </div>
                    <div class="text-muted font-anton text-xs tracking-wide">ACTUALIZADO: 20 ENE 2025</div>
                </div>
            </div>
        </div>
    </section>
</body>
</html>""",

    "cliente/notificaciones.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Notificaciones - Energy Gym</title></head>
<body>
    <section class="client-wrapper">
        <div th:replace="~{fragments/cliente-sidebar :: sidebar('notificaciones')}"></div>
        <div class="client-main">
            <div th:replace="~{fragments/cliente-topbar :: topbar}"></div>
            <div class="client-content">
                <div class="section-subtitle mb-3 text-primary text-2xs">TUS NOTIFICACIONES</div>
                <h1 class="display-3 font-anton leading-none mb-1 text-light">NOTIFICACIONES</h1>
                <p class="font-anton text-muted mb-5 tracking-wide text-lg">INFORMACIÓN IMPORTANTE PARA TI</p>
                
                <div class="d-flex gap-2 mb-4">
                    <button class="btn btn-primary px-4 py-2 font-anton text-sm">TODAS (2)</button>
                    <button class="btn btn-outline px-4 py-2 font-anton text-muted text-sm">MEMBRESÍA (1)</button>
                    <button class="btn btn-outline px-4 py-2 font-anton text-muted text-sm">RESERVAS (1)</button>
                    <button class="btn btn-outline px-4 py-2 font-anton text-muted text-sm">PROMOCIONES (0)</button>
                </div>
                
                <div class="d-flex flex-column gap-3">
                    <div class="card p-4 position-relative">
                        <div class="notification-line"></div>
                        <div class="d-flex justify-content-between align-items-baseline mb-2">
                            <h5 class="font-anton m-0 text-light">TU MEMBRESÍA ESTÁ POR VENCER</h5>
                            <span class="text-muted text-xs font-anton tracking-wide">23 Ene 2025</span>
                        </div>
                        <p class="text-muted text-sm m-0">Te quedan 15 días de membresía. Renueva tu plan.</p>
                    </div>
                    <div class="card p-4">
                        <div class="d-flex justify-content-between align-items-baseline mb-2">
                            <h5 class="font-anton m-0 text-light">RESERVA CONFIRMADA</h5>
                            <span class="text-muted text-xs font-anton tracking-wide">18 Ene 2025</span>
                        </div>
                        <p class="text-muted text-sm m-0">Tu reserva #RES-00473 ha sido confirmada.</p>
                    </div>
                    <div class="card p-4">
                        <div class="d-flex justify-content-between align-items-baseline mb-2">
                            <h5 class="font-anton m-0 text-light">NUEVA PROMOCIÓN DISPONIBLE</h5>
                            <span class="text-muted text-xs font-anton tracking-wide">10 Ene 2025</span>
                        </div>
                        <p class="text-muted text-sm m-0">Aprovecha nuestra promoción de 2 meses por el precio de 1.</p>
                    </div>
                </div>
            </div>
        </div>
    </section>
</body>
</html>""",

    "cliente/detalle-producto.html": """<!DOCTYPE html>
<html th:replace="~{fragments/base :: layout(~{::title}, ~{::section})}">
<head><title>Detalle de Producto - Energy Gym</title></head>
<body>
    <section class="client-wrapper">
        <div th:replace="~{fragments/cliente-sidebar :: sidebar('productos')}"></div>
        <div class="client-main">
            <div th:replace="~{fragments/cliente-topbar :: topbar}"></div>
            <div class="client-content">
                <nav aria-label="breadcrumb" class="mb-5">
                    <ol class="breadcrumb m-0 font-anton text-sm tracking-wide">
                        <li class="breadcrumb-item"><a href="#" class="text-muted text-decoration-none">PRODUCTOS</a></li>
                        <li class="breadcrumb-item"><a href="#" class="text-muted text-decoration-none">SUPLEMENTOS</a></li>
                        <li class="breadcrumb-item active text-light" aria-current="page">PROTEÍNA WHEY</li>
                    </ol>
                </nav>
                
                <div class="grid-2-col">
                    <div>
                        <div class="card product-detail-box border-secondary mb-4">
                            <div class="product-jar">
                                <div class="product-jar-lid"></div>
                                <h1 class="font-anton text-light m-0 text-center text-3xl leading-none">WHEY<br><span class="text-primary text-xl">PROTEIN</span></h1>
                            </div>
                        </div>
                        <div class="d-flex gap-3">
                            <div class="thumbnail-box active"><span class="font-anton text-primary">01</span></div>
                            <div class="thumbnail-box"><span class="font-anton text-primary">02</span></div>
                            <div class="thumbnail-box"><span class="font-anton text-primary">03</span></div>
                        </div>
                    </div>
                    <div class="d-flex flex-column">
                        <span class="text-muted font-anton mb-2 text-sm tracking-wide">SUPLEMENTOS</span>
                        <h1 class="display-4 font-anton text-light mb-3 leading-none">WHEY PROTEIN<br>2 LB</h1>
                        <p class="text-muted text-sm mb-5 w-75">Proteína de suero de alta calidad. Ideal para apoyar el crecimiento y la recuperación muscular.</p>
                        
                        <div class="d-flex align-items-center gap-4 mb-5">
                            <h1 class="font-anton text-primary m-0">S/ 149</h1>
                            <span class="border border-secondary px-3 py-2 text-muted text-sm font-anton">12 unidades</span>
                        </div>
                        
                        <label class="form-label mb-2">CANTIDAD</label>
                        <div class="d-flex gap-3 mb-5">
                            <div class="d-flex align-items-center border border-secondary" style="width: 120px;">
                                <button class="btn btn-link text-light text-decoration-none px-3">-</button>
                                <input type="text" class="form-control text-center border-0 bg-transparent text-light p-0" value="1">
                                <button class="btn btn-link text-light text-decoration-none px-3">+</button>
                            </div>
                        </div>
                        
                        <button class="btn btn-primary w-100 d-flex justify-content-between align-items-center py-3 mb-4">
                            CONFIRMAR RESERVA <i class="fa-solid fa-arrow-right"></i>
                        </button>
                        
                        <div class="card p-3 bg-transparent border-secondary">
                            <div class="d-flex align-items-center gap-2 mb-2">
                                <i class="fa-solid fa-circle-info text-primary"></i>
                                <span class="text-primary font-anton text-sm tracking-wide">PAGO Y RECOJO PRESENCIAL</span>
                            </div>
                            <p class="text-muted text-sm m-0 ps-4">No contamos con pagos en línea.</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
</body>
</html>"""
}

for rel_path, content in templates.items():
    full_path = os.path.join(base_dir, "templates", rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w") as f:
        f.write(content)

print("Refactor complete.")
