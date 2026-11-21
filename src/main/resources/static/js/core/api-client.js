/**
 * core/api-client.js - Energy Gym Secure HTTP Client
 * Responsabilidad: Manejo centralizado de peticiones HTTP con inyección automática de tokens CSRF.
 */

export function getCsrfHeaders(includeJsonContentType = true) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    const headers = {};
    if (includeJsonContentType) {
        headers["Content-Type"] = "application/json";
    }
    headers["Accept"] = "application/json";
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

export async function getJson(url) {
    return fetch(url, {
        method: "GET",
        headers: getCsrfHeaders(false)
    });
}

export async function postJson(url, data) {
    const headers = getCsrfHeaders(true);
    return fetch(url, {
        method: "POST",
        headers,
        body: JSON.stringify(data)
    });
}

export async function putJson(url, data) {
    const headers = getCsrfHeaders(true);
    return fetch(url, {
        method: "PUT",
        headers,
        body: JSON.stringify(data)
    });
}

export async function patchRequest(url, data = null) {
    const hasBody = data !== null && data !== undefined;
    const headers = getCsrfHeaders(hasBody);
    const options = {
        method: "PATCH",
        headers
    };
    if (hasBody) {
        options.body = JSON.stringify(data);
    }
    return fetch(url, options);
}
