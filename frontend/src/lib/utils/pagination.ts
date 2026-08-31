export const DEFAULT_PAGE = 0;
export const DEFAULT_SIZE = 20;
export const MAX_PAGE_SIZE = 100;

export function parsePageParam(value?: string) {
  return parseIntegerParam(value, DEFAULT_PAGE, {
    min: 0,
    max: Number.MAX_SAFE_INTEGER,
  });
}

export function parseSizeParam(value?: string, fallback = DEFAULT_SIZE) {
  return parseIntegerParam(value, fallback, {
    min: 1,
    max: MAX_PAGE_SIZE,
  });
}

export function hasPreviousPage(page?: number) {
  return (page ?? DEFAULT_PAGE) > 0;
}

export function hasNextPage({
  hasNext,
  page,
  totalPages,
}: {
  hasNext?: boolean;
  page?: number;
  totalPages?: number;
}) {
  if (hasNext !== undefined) {
    return hasNext;
  }

  if (!totalPages) {
    return false;
  }

  return (page ?? DEFAULT_PAGE) + 1 < totalPages;
}

function parseIntegerParam(
  value: string | undefined,
  fallback: number,
  {
    max,
    min,
  }: {
    max: number;
    min: number;
  }
) {
  const parsed = Number.parseInt(value ?? "", 10);

  if (!Number.isFinite(parsed)) {
    return fallback;
  }

  return Math.min(Math.max(parsed, min), max);
}
