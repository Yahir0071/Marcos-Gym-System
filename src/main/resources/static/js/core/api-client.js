/**
 * core/api-client.js - Energy Gym Secure HTTP Client
 * Responsabilidad: Manejo centralizado de peticiones HTTP con inyección automática de tokens CSRF.
 */

export function getCsrfHeaders() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    const headers = { "Content-Type": "application/json" };
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

export async function postJson(url, data) {
    const headers = getCsrfHeaders();
    const response = await fetch(url, {
        method: "POST",
        headers: headers,
        body: JSON.stringify(data)
    });
    return response;
}
