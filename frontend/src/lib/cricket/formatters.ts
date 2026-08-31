import type { components } from "@/lib/api/schema";

type AwardType = NonNullable<
  components["schemas"]["PlayerAwardResponse"]["awardType"]
>;

export function formatPoints(value?: number) {
  return formatDecimal(value, 1);
}

export function formatNetRunRate(value?: number) {
  if (value === undefined) {
    return "NRR 0.000";
  }

  const prefix = value > 0 ? "+" : "";

  return `NRR ${prefix}${formatDecimal(value, 3)}`;
}

export function formatRunRate(value?: number) {
  return formatDecimal(value, 2);
}

export function formatAwardType(value?: AwardType) {
  switch (value) {
    case "PLAYER_OF_TOURNAMENT":
      return "Player of the Tournament";
    case "FINAL_MVP":
      return "Final MVP";
    case "BEST_FIELDER":
      return "Best Fielder";
    case "EMERGING_PLAYER":
      return "Emerging Player";
    case "CUSTOM":
      return "Custom";
    default:
      return "Award";
  }
}

function formatDecimal(value: number | undefined, fractionDigits: number) {
  return new Intl.NumberFormat("en", {
    maximumFractionDigits: fractionDigits,
    minimumFractionDigits: fractionDigits,
  }).format(value ?? 0);
}
