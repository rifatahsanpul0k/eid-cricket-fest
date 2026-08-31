import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { ShieldAlertIcon } from "lucide-react";

import { SiteFooter } from "@/components/layout/site-footer";
import { SiteHeader } from "@/components/layout/site-header";
import { ScorerConsole } from "@/components/scorer/scorer-console";
import { buttonVariants } from "@/components/ui/button";
import { getSession } from "@/lib/auth/session";
import { hasScorerAccess } from "@/lib/dashboard/roles";
import { getScorerMatchState } from "@/lib/scorer/scorer-api";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "Scorer Match",
};

type ScorerMatchPageProps = {
  params: Promise<{
    matchId: string;
  }>;
};

export default async function ScorerMatchPage({
  params,
}: ScorerMatchPageProps) {
  const matchId = parseMatchId((await params).matchId);

  if (!matchId) {
    notFound();
  }

  const session = await getSession();

  if (!session) {
    redirect(`/login?returnTo=/scorer/matches/${matchId}`);
  }

  if (!hasScorerAccess(session)) {
    return <ScorerShell><ForbiddenScorer /></ScorerShell>;
  }

  const state = await getScorerMatchState(matchId);

  if (!state.ok) {
    if (state.status === 401) {
      redirect(`/login?returnTo=/scorer/matches/${matchId}`);
    }

    if (state.status === 404) {
      notFound();
    }

    return (
      <ScorerShell>
        <ForbiddenScorer message="The backend rejected scorer access for this match." />
      </ScorerShell>
    );
  }

  return (
    <ScorerShell>
      <ScorerConsole initialState={state.data} />
    </ScorerShell>
  );
}

function ScorerShell({ children }: { children: React.ReactNode }) {
  return (
    <>
      <SiteHeader />
      {children}
      <SiteFooter />
    </>
  );
}

function ForbiddenScorer({
  message = "You are not assigned as a scorer for this match.",
}: {
  message?: string;
}) {
  return (
    <main className="flex-1">
      <section className="mx-auto grid min-h-[52vh] w-full max-w-2xl place-items-center px-4 py-12 text-center sm:px-6 lg:px-8">
        <div>
          <ShieldAlertIcon className="mx-auto size-10 text-destructive" />
          <h1 className="mt-4 font-heading text-3xl font-bold uppercase tracking-normal">
            Scorer access denied
          </h1>
          <p className="mt-3 text-sm text-muted-foreground">{message}</p>
          <Link
            className={cn(buttonVariants({ variant: "outline" }), "mt-6")}
            href="/scorer"
          >
            Back to scorer
          </Link>
        </div>
      </section>
    </main>
  );
}

function parseMatchId(value: string) {
  const parsed = Number.parseInt(value, 10);

  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}
