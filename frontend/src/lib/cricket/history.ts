import type { HistoryEdition } from "@/lib/api/history";

export function sortHistoryEditions(editions: HistoryEdition[]) {
  return [...editions].sort((left, right) => {
    const rightTime = Date.parse(right.startDate ?? "");
    const leftTime = Date.parse(left.startDate ?? "");
    const safeRightTime = Number.isNaN(rightTime) ? 0 : rightTime;
    const safeLeftTime = Number.isNaN(leftTime) ? 0 : leftTime;

    if (safeRightTime !== safeLeftTime) {
      return safeRightTime - safeLeftTime;
    }

    return (right.editionId ?? 0) - (left.editionId ?? 0);
  });
}
