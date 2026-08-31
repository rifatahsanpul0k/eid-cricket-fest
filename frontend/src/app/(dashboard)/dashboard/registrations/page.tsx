import type { Metadata } from "next";
import Link from "next/link";
import { connection } from "next/server";

import { PublicPagination } from "@/components/cricket/public-pagination";
import { ReviewSubmitButton } from "@/components/dashboard/review-submit-button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { RegistrationResponse } from "@/lib/api/schema-helpers";
import { searchOrganizerRegistrations } from "@/lib/dashboard/api";
import {
  registrationStatusLabel,
  registrationStatuses,
} from "@/lib/dashboard/status";
import { parseRegistrationSearch } from "@/lib/dashboard/search";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { formatDateTime } from "@/lib/utils/format";

export const metadata: Metadata = {
  title: "Registrations | Dashboard",
};

type RegistrationsPageProps = {
  searchParams: Promise<{
    category?: string;
    direction?: string;
    error?: string;
    page?: string;
    q?: string;
    size?: string;
    sortBy?: string;
    status?: string;
  }>;
};

export default async function RegistrationsPage({
  searchParams,
}: RegistrationsPageProps) {
  await connection();

  const params = await searchParams;
  const search = parseRegistrationSearch(params);
  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status !== "ready") {
    return <Unavailable message={currentEdition.message} />;
  }

  const registrations = await searchOrganizerRegistrations(
    currentEdition.edition.id,
    search
  );

  return (
    <main className="flex-1">
      <DashboardHeader
        description={`${currentEdition.edition.name} registration review`}
        title="Registrations"
      />
      <section className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {params.error ? (
          <p className="mb-4 rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}
        <RegistrationFilters search={search} />
        {registrations.ok ? (
          <>
            <RegistrationTable
              registrations={registrations.data.content ?? []}
              returnTo={returnPath("/dashboard/registrations", params)}
            />
            <PublicPagination
              basePath="/dashboard/registrations"
              hasNext={registrations.data.hasNext}
              hasPrevious={registrations.data.hasPrevious}
              page={registrations.data.page ?? search.page ?? 0}
              params={{
                category: search.category,
                direction: search.direction,
                q: search.q,
                size: String(search.size ?? 20),
                sortBy: search.sortBy,
                status: search.status,
              }}
              totalPages={registrations.data.totalPages}
            />
          </>
        ) : (
          <Unavailable
            message={
              registrations.status === 403
                ? "Organizer or admin access is required."
                : registrations.error.detail ?? registrations.error.title
            }
          />
        )}
      </section>
    </main>
  );
}

function DashboardHeader({
  description,
  title,
}: {
  description: string;
  title: string;
}) {
  return (
    <section className="border-b border-white/10 bg-background">
      <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <p className="font-mono text-xs uppercase text-primary">Organizer</p>
        <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
          {title}
        </h1>
        <p className="mt-3 text-sm text-muted-foreground">{description}</p>
      </div>
    </section>
  );
}

function RegistrationFilters({
  search,
}: {
  search: ReturnType<typeof parseRegistrationSearch>;
}) {
  return (
    <form
      action="/dashboard/registrations"
      className="grid gap-3 rounded-sm border border-white/10 bg-card p-4 md:grid-cols-[1fr_auto_auto_auto_auto]"
      method="get"
    >
      <FilterLabel label="Search">
        <input
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.q}
          name="q"
          placeholder="Player name"
          type="search"
        />
      </FilterLabel>
      <FilterLabel label="Status">
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.status ?? ""}
          name="status"
        >
          <option value="">All</option>
          {registrationStatuses.map((status) => (
            <option key={status} value={status}>
              {registrationStatusLabel(status)}
            </option>
          ))}
        </select>
      </FilterLabel>
      <FilterLabel label="Category">
        <input
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.category}
          name="category"
          placeholder="Code"
        />
      </FilterLabel>
      <FilterLabel label="Sort">
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.sortBy}
          name="sortBy"
        >
          <option value="registeredAt">Registered</option>
          <option value="status">Status</option>
          <option value="createdAt">Created</option>
        </select>
      </FilterLabel>
      <FilterLabel label="Direction">
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.direction}
          name="direction"
        >
          <option value="desc">Newest first</option>
          <option value="asc">Oldest first</option>
        </select>
      </FilterLabel>
      <button
        className="min-h-11 rounded-sm bg-secondary px-4 text-sm font-semibold text-secondary-foreground transition-colors hover:bg-secondary/85 md:col-start-5"
        type="submit"
      >
        Apply
      </button>
    </form>
  );
}

function RegistrationTable({
  registrations,
  returnTo,
}: {
  registrations: RegistrationResponse[];
  returnTo: string;
}) {
  if (registrations.length === 0) {
    return (
      <div className="mt-6 rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
        No registrations match this view.
      </div>
    );
  }

  return (
    <div className="mt-6 rounded-sm border border-white/10 bg-card p-3">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Player</TableHead>
            <TableHead>Category</TableHead>
            <TableHead>Fee</TableHead>
            <TableHead>Registered</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {registrations.map((registration) => (
            <TableRow key={registration.id}>
              <TableCell>
                {registration.playerId ? (
                  <Link
                    className="font-medium text-primary hover:underline"
                    href={`/players/${registration.playerId}`}
                  >
                    Player #{registration.playerId}
                  </Link>
                ) : (
                  "Player unavailable"
                )}
                <p className="mt-1 text-xs text-muted-foreground">
                  Registration #{registration.id}
                </p>
              </TableCell>
              <TableCell>{registration.category ?? "TBD"}</TableCell>
              <TableCell>
                {registration.feeAmount ?? 0} {registration.currency ?? ""}
              </TableCell>
              <TableCell>{formatDateTime(registration.registeredAt)}</TableCell>
              <TableCell>
                <Badge variant="outline">
                  {registrationStatusLabel(registration.status)}
                </Badge>
              </TableCell>
              <TableCell>
                {registration.status === "PENDING" && registration.id ? (
                  <div className="flex min-w-48 flex-col gap-2">
                    <form action="/api/dashboard/registrations" method="post">
                      <input name="action" type="hidden" value="approve" />
                      <input
                        name="registrationId"
                        type="hidden"
                        value={registration.id}
                      />
                      <input name="returnTo" type="hidden" value={returnTo} />
                      <ReviewSubmitButton>Approve</ReviewSubmitButton>
                    </form>
                    <details>
                      <summary className="cursor-pointer text-sm text-destructive">
                        Reject
                      </summary>
                      <form
                        action="/api/dashboard/registrations"
                        className="mt-2 grid gap-2"
                        method="post"
                      >
                        <input name="action" type="hidden" value="reject" />
                        <input
                          name="registrationId"
                          type="hidden"
                          value={registration.id}
                        />
                        <input name="returnTo" type="hidden" value={returnTo} />
                        <textarea
                          className="min-h-20 rounded-sm border border-white/10 bg-background px-3 py-2 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                          maxLength={1000}
                          name="reason"
                          placeholder="Reason"
                          required
                        />
                        <ReviewSubmitButton variant="destructive">
                          Reject
                        </ReviewSubmitButton>
                      </form>
                    </details>
                  </div>
                ) : (
                  <span className="text-sm text-muted-foreground">
                    No action
                  </span>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function FilterLabel({
  children,
  label,
}: {
  children: React.ReactNode;
  label: string;
}) {
  return (
    <label className="grid gap-2 text-sm">
      <span className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </span>
      {children}
    </label>
  );
}

function Unavailable({ message }: { message: string }) {
  return (
    <main className="flex-1">
      <section className="mx-auto w-full max-w-3xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
          {message}
        </div>
      </section>
    </main>
  );
}

function returnPath(
  path: string,
  params: Record<string, string | undefined>
) {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (key !== "error" && value) {
      search.set(key, value);
    }
  });

  const query = search.toString();

  return query ? `${path}?${query}` : path;
}
