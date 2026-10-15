/**
 * public/calculator.js - Energy Gym Fitness & BMI Calculator
 * Responsabilidad: Cálculo de IMC, tasa calórica y recomendación personalizada de planes.
 */

import { showGymToast } from "../core/toast.js";

export function initBmiCalculator() {
    const bmiForm = document.getElementById("bmiCalculatorForm");
    if (!bmiForm) return;

    bmiForm.addEventListener("submit", function (e) {
        e.preventDefault();
        const weight = parseFloat(document.getElementById("bmiWeight")?.value) || 74;
        const heightCm = parseFloat(document.getElementById("bmiHeight")?.value) || 175;
        const goal = document.getElementById("bmiGoal")?.value || "mantenimiento";

        const heightM = heightCm / 100;
        const bmi = (weight / (heightM * heightM)).toFixed(1);

        let status = "PESO SALUDABLE";
        if (bmi < 18.5) status = "BAJO PESO — ENFOQUE VOLUMEN";
        else if (bmi >= 25 && bmi < 30) status = "SOBREPESO / POTENCIAL MUSCULAR";
        else if (bmi >= 30) status = "ENFOQUE QUEMA DE GRASA";

        let calories = Math.round(weight * 32);
        let advice = "";
        let recPlan = "PLAN PLUS (S/ 119)";

        if (goal === "hipertrofia") {
            calories = Math.round(weight * 36);
            advice = `Para ganar masa muscular (${Math.round(weight * 1.8)}g de proteína/día), te recomendamos entrenar 4-5 días en la Zona de Fuerza y complementar con Whey Protein 2 LB + Creatina.`;
            recPlan = "PLAN PREMIUM (S/ 159)";
        } else if (goal === "definicion") {
            calories = Math.round(weight * 27);
            advice = `Déficit calórico controlado (${calories} kcal/día) combinando pesas y 25 min en Zona Cardio HIIT. Te sugerimos Whey Protein aislada cero carbohidratos.`;
            recPlan = "PLAN PLUS (S/ 119)";
        } else {
            calories = Math.round(weight * 31);
            advice = `Mantenimiento energético óptimo (${calories} kcal/día) alternando circuitos funcionales, remo y musculación general 3-4 veces por semana.`;
            recPlan = "PLAN BÁSICO (S/ 79)";
        }

        const scoreEl = document.getElementById("bmiScoreText");
        const badgeEl = document.getElementById("bmiCalorieBadge");
        const adviceEl = document.getElementById("bmiAdviceText");
        const planLinkEl = document.getElementById("bmiPlanLink");

        if (scoreEl) scoreEl.textContent = `IMC: ${bmi} — ${status}`;
        if (badgeEl) badgeEl.textContent = `${calories.toLocaleString()} KCAL / DÍA`;
        if (adviceEl) adviceEl.textContent = advice;
        if (planLinkEl) planLinkEl.textContent = `RECOMENDADO: ${recPlan} →`;

        showGymToast(`Diagnóstico calculado: IMC ${bmi} (${calories} kcal/día)`, "fa-heart-pulse");
    });
}
