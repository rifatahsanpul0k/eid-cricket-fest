import Link from "next/link";

import { hasNextPage, hasPreviousPage } from "@/lib/utils/pagination";

export function PublicPagination({
  basePath,
  page,
  params = {},
  totalPages,
  hasNext,
  hasPrevious,
}: {
  basePath: string;
  page: number;
  params?: Record<string, string | undefined>;
  totalPages?: number;
  hasNext?: boolean;
  hasPrevious?: boolean;
}) {
  const previousEnabled = hasPrevious ?? hasPreviousPage(page);
  const nextEnabled = hasNextPage({ hasNext, page, totalPages });

  return (
    <nav
      aria-label="Pagination"
      className="mt-6 flex flex-col gap-3 border-t border-white/10 pt-5 sm:flex-row sm:items-center sm:justify-between"
    >
      <p className="font-mono text-xs uppercase text-muted-foreground">
        Page {page + 1}
        {totalPages ? ` of ${totalPages}` : ""}
      </p>
      <div className="flex gap-2">
        <PageLink
          disabled={!previousEnabled}
          href={pageHref(basePath, { ...params, page: String(Math.max(page - 1, 0)) })}
          label="Previous"
        />
        <PageLink
          disabled={!nextEnabled}
          href={pageHref(basePath, { ...params, page: String(page + 1) })}
          label="Next"
        />
      </div>
    </nav>
  );
}

function PageLink({
  disabled,
  href,
  label,
}: {
  disabled: boolean;
  href: string;
  label: string;
}) {
  if (disabled) {
    return (
      <span className="rounded-sm border border-white/10 px-3 py-2 text-sm text-muted-foreground opacity-50">
        {label}
      </span>
    );
  }

  return (
    <Link
      className="rounded-sm border border-white/10 px-3 py-2 text-sm font-medium transition-colors hover:bg-surface-elevated"
      href={href}
    >
      {label}
    </Link>
  );
}

function pageHref(basePath: string, params: Record<string, string | undefined>) {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value) {
      search.set(key, value);
    }
  });

  const query = search.toString();

  return query ? `${basePath}?${query}` : basePath;
}
