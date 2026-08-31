import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { ShieldAlertIcon } from "lucide-react";

import { SiteFooter } from "@/components/layout/site-footer";
import { SiteHeader } from "@/components/layout/site-header";
import { buttonVariants } from "@/components/ui/button";
import { getSession } from "@/lib/auth/session";
import { hasScorerAccess } from "@/lib/dashboard/roles";
import { getScorerMatches } from "@/lib/scorer/scorer-api";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "Scorer Console",
};

export default async function ScorerPage() {
  const session = await getSession();

  if (!session) {
    redirect("/login?returnTo=/scorer");
  }

  if (!hasScorerAccess(session)) {
    return <ScorerShell><ForbiddenScorer /></ScorerShell>;
  }

  const matches = await getScorerMatches();

  if (!matches.ok) {
    if (matches.status === 401) {
      redirect("/login?returnTo=/scorer");
    }

    return (
      <ScorerShell>
        <ForbiddenScorer message="The backend rejected scorer access for this account." />
      </ScorerShell>
    );
  }

  return (
    <ScorerShell>
      <main className="flex-1">
        <section className="border-b border-white/10 bg-background">
          <div className="mx-auto w-full max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
            <p className="font-mono text-xs uppercase text-primary">
              Scorer Console
            </p>
            <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
              Assigned Matches
            </h1>
          </div>
        </section>
        <section className="mx-auto grid w-full max-w-5xl gap-3 px-4 py-6 sm:px-6 lg:px-8">
          {matches.data.length > 0 ? (
            matches.data.map((item) => (
              <article
                className="rounded-sm border border-white/10 bg-card p-4"
                key={item.match?.id}
              >
                <p className="font-mono text-xs uppercase text-muted-foreground">
                  Match {item.match?.matchNumber ?? "-"} ·{" "}
                  {item.match?.status ?? "-"}
                </p>
                <h2 className="mt-2 font-heading text-2xl font-bold uppercase tracking-normal">
                  {item.match?.teamA?.name ?? "Team A"} vs{" "}
                  {item.match?.teamB?.name ?? "Team B"}
                </h2>
                <div className="mt-4 flex flex-wrap items-center gap-2">
                  <Link
                    className={buttonVariants()}
                    href={`/scorer/matches/${item.match?.id}`}
                  >
                    Open Scorer
                  </Link>
                  {item.match?.id ? (
                    <Link
                      className={buttonVariants({ variant: "outline" })}
                      href={`/matches/${item.match.id}/live`}
                    >
                      Public Live
                    </Link>
                  ) : null}
                </div>
              </article>
            ))
          ) : (
            <p className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
              No assigned scorer matches are available for this account.
            </p>
          )}
        </section>
      </main>
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
  message = "A scorer, organizer, or admin role is required.",
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
            href="/account"
          >
            Back to account
          </Link>
        </div>
      </section>
    </main>
  );
}
