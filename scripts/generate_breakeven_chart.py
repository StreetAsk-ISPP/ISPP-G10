"""
Generate the "Recuperación de la Inversión" chart for the PPL deliverable.

Scenarios:
  - Optimista (10.000 MAU, 88% Gr / 6% Pr / 3% Bs) — kept as-is
  - Esperado  ( 2.500 MAU, 94% Gr / 4% Pr / 2% Bs) — kept as-is
  - Pesimista ( 1.500 MAU, 95% Gr / 4% Pr / 1% Bs) — revised: ~71 months

Output: docs/PPL/costs/breakeven_recovery.png
"""

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

# --- Inversión a recuperar ---------------------------------------------------
INVESTMENT = 84_181.38  # EUR

# --- Precios -----------------------------------------------------------------
ADS_ARPU      = 0.60   # ingreso medio por usuario gratuito (ads), EUR/mes
PREMIUM_PRICE = 2.99   # EUR/mes
BUSINESS_PRICE = 19.99 # EUR/mes


def arpu(p_free: float, p_premium: float, p_business: float) -> float:
    """ARPU mensual a partir del mix de usuarios."""
    return (p_free * ADS_ARPU
            + p_premium * PREMIUM_PRICE
            + p_business * BUSINESS_PRICE)


SCENARIOS = [
    {
        "name": "Pesimista",
        "mau": 1_500,
        "mix": (0.95, 0.04, 0.01),
        "opex": 150.0,
        "color": "#d62728",  # red
        "label_y_offset": 8000,
    },
    {
        "name": "Esperado",
        "mau": 2_500,
        "mix": (0.94, 0.04, 0.02),
        "opex": 100.0,
        "color": "#1f77b4",  # blue
        "label_y_offset": 5500,
    },
    {
        "name": "Optimista",
        "mau": 10_000,
        "mix": (0.88, 0.06, 0.03),
        "opex": 500.0,
        "color": "#2ca02c",  # green
        "label_y_offset": 5500,
    },
]


def compute(scn: dict) -> dict:
    a = arpu(*scn["mix"])
    gross = scn["mau"] * a
    net = gross - scn["opex"]
    months_to_breakeven = INVESTMENT / net
    return {
        **scn,
        "arpu": a,
        "gross_monthly": gross,
        "net_monthly": net,
        "months_to_breakeven": months_to_breakeven,
    }


def main() -> None:
    scenarios = [compute(s) for s in SCENARIOS]

    fig, ax = plt.subplots(figsize=(16, 9), dpi=180)

    # X axis: months
    X_MAX = 75
    months = np.linspace(0, X_MAX, 400)

    # Plot scenarios
    for s in scenarios:
        y = s["net_monthly"] * months
        ax.plot(months, y, color=s["color"], linewidth=3.6,
                label=f"{s['name']}: {s['mau']:,.0f} MAU "
                      f"({s['mix'][0]*100:.0f}% Gr, "
                      f"{s['mix'][1]*100:.0f}% Pr, "
                      f"{s['mix'][2]*100:.0f}% Bs)".replace(",", "."))

    # Punto Muerto (horizontal line)
    ax.axhline(INVESTMENT, color="black", linestyle=":", linewidth=2.4,
               label=f"Punto Muerto ({INVESTMENT:,.2f} €)".replace(",", "X").replace(".", ",").replace("X", "."))

    # Annotations at breakeven crossing
    for s in scenarios:
        x = s["months_to_breakeven"]
        if x > X_MAX:
            continue
        # Bullet at the crossing
        ax.plot([x], [INVESTMENT], "o", color=s["color"], markersize=14,
                markeredgecolor="white", markeredgewidth=2.0, zorder=5)
        # Label above the crossing
        ax.annotate(
            f"~{x:.1f} MESES\n(Escenario {s['name']})",
            xy=(x, INVESTMENT),
            xytext=(x, INVESTMENT + s["label_y_offset"]),
            ha="center",
            fontsize=20,
            fontweight="bold",
            color=s["color"],
            arrowprops=dict(arrowstyle="->", color=s["color"], lw=2.0,
                            shrinkA=0, shrinkB=6),
        )

    # Axes formatting
    ax.set_xlim(0, X_MAX)
    ax.set_ylim(0, 110_000)
    ax.set_xlabel("Meses", fontsize=22, fontweight="bold", labelpad=10)
    ax.set_ylabel("Ganancia Acumulada (€)", fontsize=22, fontweight="bold", labelpad=10)
    ax.set_title("RECUPERACIÓN DE LA INVERSIÓN",
                 fontsize=34, fontweight="bold", pad=22)

    # Tick labels
    ax.tick_params(axis="both", which="major", labelsize=18, length=6, width=1.4)

    # Y-axis: thousand separator with dots (Spanish style)
    ax.yaxis.set_major_formatter(
        plt.FuncFormatter(lambda v, _: f"{v:,.0f}".replace(",", "."))
    )

    ax.grid(True, which="major", linestyle="--", alpha=0.35, linewidth=1.0)
    legend = ax.legend(loc="lower right", fontsize=16, framealpha=0.97,
                       title="ESCENARIOS & DISTRIBUCIÓN",
                       title_fontsize=17, borderpad=0.9, labelspacing=0.7)
    legend.get_title().set_fontweight("bold")

    # Print summary to stdout
    print("\nResumen:")
    print(f"  Inversión a recuperar: {INVESTMENT:,.2f} EUR")
    for s in scenarios:
        print(f"  {s['name']:<10s} | {s['mau']:>6,} MAU | "
              f"ARPU {s['arpu']:.4f} EUR | "
              f"bruto {s['gross_monthly']:>8.2f} EUR/mes | "
              f"opex {s['opex']:>6.2f} EUR/mes | "
              f"neto {s['net_monthly']:>8.2f} EUR/mes | "
              f"break-even {s['months_to_breakeven']:>6.1f} meses")

    # Output
    out_dir = Path(__file__).resolve().parent.parent / "docs" / "PPL" / "costs"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "breakeven_recovery.png"
    fig.tight_layout()
    fig.savefig(out_path, dpi=150, bbox_inches="tight", facecolor="white")
    print(f"\nGuardado: {out_path}")


if __name__ == "__main__":
    main()
