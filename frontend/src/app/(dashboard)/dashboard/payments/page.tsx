import type { Metadata } from "next";
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
import type { PaymentResponse } from "@/lib/api/schema-helpers";
import { searchOrganizerPayments } from "@/lib/dashboard/api";
import { parsePaymentSearch } from "@/lib/dashboard/search";
import {
  paymentMethodLabel,
  paymentMethods,
  paymentStatusLabel,
  paymentStatuses,
} from "@/lib/dashboard/status";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { formatDateTime } from "@/lib/utils/format";

export const metadata: Metadata = {
  title: "Payments | Dashboard",
};

type PaymentsPageProps = {
  searchParams: Promise<{
    direction?: string;
    error?: string;
    method?: string;
    page?: string;
    q?: string;
    size?: string;
    sortBy?: string;
    status?: string;
  }>;
};

export default async function PaymentsPage({ searchParams }: PaymentsPageProps) {
  await connection();

  const params = await searchParams;
  const search = parsePaymentSearch(params);
  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status !== "ready") {
    return <Unavailable message={currentEdition.message} />;
  }

  const payments = await searchOrganizerPayments(
    currentEdition.edition.id,
    search
  );

  return (
    <main className="flex-1">
      <DashboardHeader
        description={`${currentEdition.edition.name} payment review`}
        title="Payments"
      />
      <section className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {params.error ? (
          <p className="mb-4 rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}
        <PaymentFilters search={search} />
        {payments.ok ? (
          <>
            <PaymentTable
              payments={payments.data.content ?? []}
              returnTo={returnPath("/dashboard/payments", params)}
            />
            <PublicPagination
              basePath="/dashboard/payments"
              hasNext={payments.data.hasNext}
              hasPrevious={payments.data.hasPrevious}
              page={payments.data.page ?? search.page ?? 0}
              params={{
                direction: search.direction,
                method: search.method,
                q: search.q,
                size: String(search.size ?? 20),
                sortBy: search.sortBy,
                status: search.status,
              }}
              totalPages={payments.data.totalPages}
            />
          </>
        ) : (
          <Unavailable
            message={
              payments.status === 403
                ? "Organizer or admin access is required."
                : payments.error.detail ?? payments.error.title
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

function PaymentFilters({
  search,
}: {
  search: ReturnType<typeof parsePaymentSearch>;
}) {
  return (
    <form
      action="/dashboard/payments"
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
          {paymentStatuses.map((status) => (
            <option key={status} value={status}>
              {paymentStatusLabel(status)}
            </option>
          ))}
        </select>
      </FilterLabel>
      <FilterLabel label="Method">
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.method ?? ""}
          name="method"
        >
          <option value="">All</option>
          {paymentMethods.map((method) => (
            <option key={method} value={method}>
              {paymentMethodLabel(method)}
            </option>
          ))}
        </select>
      </FilterLabel>
      <FilterLabel label="Sort">
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.sortBy}
          name="sortBy"
        >
          <option value="createdAt">Submitted</option>
          <option value="paidAt">Paid</option>
          <option value="status">Status</option>
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

function PaymentTable({
  payments,
  returnTo,
}: {
  payments: PaymentResponse[];
  returnTo: string;
}) {
  if (payments.length === 0) {
    return (
      <div className="mt-6 rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
        No payments match this view.
      </div>
    );
  }

  return (
    <div className="mt-6 rounded-sm border border-white/10 bg-card p-3">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Registration</TableHead>
            <TableHead>Amount</TableHead>
            <TableHead>Method</TableHead>
            <TableHead>Reference</TableHead>
            <TableHead>Paid</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {payments.map((payment) => (
            <TableRow key={payment.id}>
              <TableCell>Registration #{payment.registrationId}</TableCell>
              <TableCell>{payment.amount ?? 0}</TableCell>
              <TableCell>{paymentMethodLabel(payment.paymentMethod)}</TableCell>
              <TableCell>
                {payment.transactionReference ?? (
                  <span className="text-muted-foreground">None</span>
                )}
                {payment.rejectionReason ? (
                  <p className="mt-1 max-w-64 whitespace-normal text-xs text-destructive">
                    {payment.rejectionReason}
                  </p>
                ) : null}
              </TableCell>
              <TableCell>{formatDateTime(payment.paidAt)}</TableCell>
              <TableCell>
                <Badge variant="outline">
                  {paymentStatusLabel(payment.status)}
                </Badge>
                {payment.verifiedAt ? (
                  <p className="mt-1 text-xs text-muted-foreground">
                    Verified {formatDateTime(payment.verifiedAt)}
                  </p>
                ) : null}
              </TableCell>
              <TableCell>
                {payment.status === "PENDING" && payment.id ? (
                  <div className="flex min-w-48 flex-col gap-2">
                    <form action="/api/dashboard/payments" method="post">
                      <input name="action" type="hidden" value="verify" />
                      <input name="paymentId" type="hidden" value={payment.id} />
                      <input name="returnTo" type="hidden" value={returnTo} />
                      <ReviewSubmitButton>Verify</ReviewSubmitButton>
                    </form>
                    <details>
                      <summary className="cursor-pointer text-sm text-destructive">
                        Reject
                      </summary>
                      <form
                        action="/api/dashboard/payments"
                        className="mt-2 grid gap-2"
                        method="post"
                      >
                        <input name="action" type="hidden" value="reject" />
                        <input
                          name="paymentId"
                          type="hidden"
                          value={payment.id}
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
