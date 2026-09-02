import type { Metadata } from "next";
import Link from "next/link";
import { ShieldAlertIcon } from "lucide-react";

import { FriendlyMatchForm } from "@/components/dashboard/friendly-match-form";
import { buttonVariants } from "@/components/ui/button";
import { getSession } from "@/lib/auth/session";
import {
  getFriendlyPlayerOptions,
  getVenues,
} from "@/lib/dashboard/match-admin-api";
import { hasOrganizerAccess } from "@/lib/dashboard/roles";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "New Friendly Match | Dashboard",
};

type NewFriendlyMatchPageProps = {
  searchParams: Promise<{ error?: string }>;
};

export default async function NewFriendlyMatchPage({
  searchParams,
}: NewFriendlyMatchPageProps) {
  const [query, session, venues, players] = await Promise.all([
    searchParams,
    getSession(),
    getVenues(),
    getFriendlyPlayerOptions(),
  ]);

  if (!hasOrganizerAccess(session)) {
    return <ForbiddenDashboard />;
  }

  return (
    <main className="flex-1">
      <section className="border-b border-white/10 bg-background">
        <div className="mx-auto flex w-full max-w-7xl flex-wrap items-end justify-between gap-4 px-4 py-10 sm:px-6 lg:px-8">
          <div>
            <p className="font-mono text-xs uppercase text-primary">
              Organizer
            </p>
            <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
              New Friendly Match
            </h1>
            <p className="mt-3 text-sm text-muted-foreground">
              Create a standalone match from existing player profiles.
            </p>
          </div>
          <Link
            className={cn(buttonVariants({ variant: "outline" }), "h-9")}
            href="/dashboard/matches"
          >
            Back to matches
          </Link>
        </div>
      </section>
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:px-8">
        {query.error ? (
          <p className="rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {query.error}
          </p>
        ) : null}
        {!venues.ok ? (
          <Unavailable message={venues.error.detail ?? venues.error.title} />
        ) : !players.ok ? (
          <Unavailable message={players.error.detail ?? players.error.title} />
        ) : (
          <FriendlyMatchForm
            action="/api/dashboard/friendly-matches"
            players={players.data}
            venues={venues.data}
          />
        )}
      </section>
    </main>
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
            Organizer or admin access is required for match operations.
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

function Unavailable({ message }: { message: string }) {
  return (
    <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
      {message}
    </div>
  );
}
