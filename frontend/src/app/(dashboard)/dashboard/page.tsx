import type { Metadata } from "next";
import Link from "next/link";
import { ShieldAlertIcon } from "lucide-react";

import { buttonVariants } from "@/components/ui/button";
import { getSession } from "@/lib/auth/session";
import { searchOrganizerPayments, searchOrganizerRegistrations } from "@/lib/dashboard/api";
import { hasOrganizerAccess } from "@/lib/dashboard/roles";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "Dashboard",
};

export default async function DashboardPage() {
  const [session, currentEdition] = await Promise.all([
    getSession(),
    getCurrentEditionData(),
  ]);

  if (!hasOrganizerAccess(session)) {
    return <ForbiddenDashboard />;
  }

  const [registrations, payments] =
    currentEdition.status === "ready"
      ? await Promise.all([
          searchOrganizerRegistrations(currentEdition.edition.id, {
            page: 0,
            size: 1,
          }),
          searchOrganizerPayments(currentEdition.edition.id, {
            page: 0,
            size: 1,
          }),
        ])
      : [undefined, undefined];

  if (registrations && !registrations.ok && registrations.status === 403) {
    return <ForbiddenDashboard />;
  }

  return (
    <main className="flex-1">
      <DashboardHeader
        description={
          currentEdition.status === "ready"
            ? `${currentEdition.edition.name} operations`
            : currentEdition.message
        }
        title="Dashboard"
      />
      <section className="mx-auto grid w-full max-w-5xl gap-4 px-4 py-8 sm:px-6 md:grid-cols-2 lg:px-8">
        <DashboardCard
          description="Create permanent teams, add teams to the current edition, and assign captains."
          href="/dashboard/teams"
          label="Teams"
        />
        <DashboardCard
          description="Manage draft lifecycle, order, eligible pool, picks, and backend-provided rosters."
          href="/dashboard/draft"
          label="Draft"
        />
        <DashboardCard
          description="Generate league fixtures, create venues, and inspect knockout bracket setup."
          href="/dashboard/fixtures"
          label="Fixtures"
        />
        <DashboardCard
          description="Schedule matches, assign scorers, submit playing XIs, and record tosses."
          href="/dashboard/matches"
          label="Matches"
        />
        <DashboardCard
          count={registrations?.ok ? registrations.data.totalElements : undefined}
          description="Review player registrations and approve or reject pending requests."
          href="/dashboard/registrations"
          label="Registrations"
        />
        <DashboardCard
          count={payments?.ok ? payments.data.totalElements : undefined}
          description="Review submitted registration payments and verify or reject pending entries."
          href="/dashboard/payments"
          label="Payments"
        />
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

function DashboardCard({
  count,
  description,
  href,
  label,
}: {
  count?: number;
  description: string;
  href: string;
  label: string;
}) {
  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        {count === undefined ? "Review" : `${count} total`}
      </p>
      <h2 className="mt-3 font-heading text-2xl font-bold uppercase tracking-normal">
        {label}
      </h2>
      <p className="mt-3 text-sm text-muted-foreground">{description}</p>
      <Link className={cn(buttonVariants(), "mt-5")} href={href}>
        Open
      </Link>
    </article>
  );
}

function ForbiddenDashboard() {
  return (
    <main className="flex-1">
      <section className="mx-auto grid min-h-[52vh] w-full max-w-2xl place-items-center px-4 py-12 text-center sm:px-6 lg:px-8">
        <div>
          <ShieldAlertIcon className="mx-auto size-10 text-destructive" />
          <h1 className="mt-4 font-heading text-3xl font-bold uppercase tracking-normal">
            Dashboard access denied
          </h1>
          <p className="mt-3 text-sm text-muted-foreground">
            Organizer or admin access is required for registration operations.
          </p>
          <Link
            className={cn(buttonVariants({ variant: "outline" }), "mt-6")}
            href="/account"
          >
            Back to account
          </Link>
        </div>
      </section>
    </main>
  );
}
