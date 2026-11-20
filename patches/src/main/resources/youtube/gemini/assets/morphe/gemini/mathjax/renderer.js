/* Offline Gemini math rendering. MathJax 3.2.2 is distributed separately under LICENSE. */
"use strict";

window.MathJax = {
    startup: {
        typeset: false,
        pageReady() {
            GeminiMath.ready();
        }
    },
    options: { enableMenu: false },
    // tex-svg-full already contains the extensions and SVG font paths. Do not fetch packages.
    tex: {
        packages: { "[-]": ["autoload", "require"] },
        formatError(_jax, error) { throw error; }
    },
    svg: { fontCache: "none" }
};

const formulaCache = new Map();
let currentRevision = -1;

function reportHeight() {
    if (currentRevision >= 0) {
        GeminiMath.rendered(currentRevision, Math.ceil(document.body.getBoundingClientRect().height));
    }
}

// ResizeObserver also handles rotation and text wrapping after the native view changes width.
new ResizeObserver(reportHeight).observe(document.documentElement);
window.addEventListener("error", () => GeminiMath.failed());

/** HTML comes from Android's escaped Spanned serializer; TeX is passed as data, never as script. */
window.renderMessage = function (html, formulas, style, revision) {
    try {
        currentRevision = revision;
        document.body.style.color = style.color;
        document.body.style.fontSize = style.size + "px";
        document.body.style.padding = style.padding.map(value => value + "px").join(" ");
        document.body.innerHTML = html;
        const width = document.body.clientWidth;
        for (const element of document.querySelectorAll("[data-math]")) {
            const formula = formulas[Number(element.dataset.math)];
            element.classList.toggle("block", formula.block);
            // Repeated streaming chunks reuse completed formulas, without retaining unbounded history.
            const key = JSON.stringify([formula.latex, formula.block, style.size, width]);
            let svg = formulaCache.get(key);
            if (svg === undefined) {
                try {
                    MathJax.texReset();
                    svg = MathJax.tex2svg(formula.latex, {
                        display: formula.block,
                        em: style.size,
                        ex: style.size / 2,
                        containerWidth: width
                    }).outerHTML;
                    if (formulaCache.size >= 128) formulaCache.delete(formulaCache.keys().next().value);
                    formulaCache.set(key, svg);
                } catch (_) {
                    // Invalid or unsupported syntax retains the exact delimited source.
                    element.textContent = formula.source;
                    continue;
                }
            }
            element.innerHTML = svg;
        }
        reportHeight();
    } catch (_) {
        GeminiMath.failed();
    }
};
