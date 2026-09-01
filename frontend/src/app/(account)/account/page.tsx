import type { Metadata } from "next";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import type {
  MyEditionStatisticsResponse,
  MyMatchResponse,
  MyTeamResponse,
  PaymentResponse,
} from "@/lib/api/schema-helpers";
import type { BackendResult } from "@/lib/auth/backend";
import { getMyProfile, getMyRegistration, getMyPayments } from "@/lib/auth/account-api";
import { getMyMatches, getMyStatistics, getMyTeam } from "@/lib/auth/my-cricket-api";
import { myMatchAction, partitionMyMatches } from "@/lib/auth/my-cricket-state";
import {
  paymentStatusMessage,
  registrationStatusMessage,
} from "@/lib/auth/account-state";
import { getSession } from "@/lib/auth/session";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "Account",
};

export default async function AccountPage() {
  const [session, currentEdition, profileResult] = await Promise.all([
    getSession(),
    getCurrentEditionData(),
    getMyProfile(),
  ]);
  const profile = profileResult && "ok" in profileResult && profileResult.ok
    ? profileResult.data
    : undefined;
  const registrationResult =
    currentEdition.status === "ready"
      ? await getMyRegistration(currentEdition.edition.id)
      : undefined;
  const registration =
    registrationResult && "ok" in registrationResult && registrationResult.ok
      ? registrationResult.data
      : undefined;
  let payments: PaymentResponse[] = [];
  let teamResult: BackendResult<MyTeamResponse> | undefined;
  let matches: MyMatchResponse[] = [];
  let statisticsResult: BackendResult<MyEditionStatisticsResponse> | undefined;

  if (currentEdition.status === "ready") {
    [payments, teamResult, matches, statisticsResult] = await Promise.all([
      registration?.id ? getMyPayments(registration.id) : [],
      getMyTeam(currentEdition.edition.id),
      getMyMatches(currentEdition.edition.id),
      getMyStatistics(currentEdition.edition.id),
    ]);
  }
  const team = teamResult && "ok" in teamResult && teamResult.ok
    ? teamResult.data
    : undefined;
  const statistics =
    statisticsResult && "ok" in statisticsResult && statisticsResult.ok
      ? statisticsResult.data
      : undefined;
  const nextMatch = partitionMyMatches(matches).upcoming[0];
  const nextMatchAction = nextMatch ? myMatchAction(nextMatch) : undefined;

  return (
    <main className="flex-1">
      <section className="border-b border-white/10 bg-background">
        <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <p className="font-mono text-xs uppercase text-primary">
            Player account
          </p>
          <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
            Account
          </h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
            {session?.user?.displayName
              ? `Signed in as ${session.user.displayName}.`
              : "Your signed-in tournament workspace."}
          </p>
        </div>
      </section>
      <section className="mx-auto grid w-full max-w-7xl gap-4 px-4 py-8 sm:px-6 md:grid-cols-2 lg:px-8">
        <AccountPanel
          actionHref="/account/profile"
          actionLabel={profile ? "View profile" : "Create profile"}
          title="Profile"
        >
          {profile ? (
            <>
              <p className="font-medium text-foreground">{profile.fullName}</p>
              <p className="mt-1 text-muted-foreground">
                {profile.primaryCategory?.name ?? "Category not selected"}
              </p>
            </>
          ) : (
            <p className="text-muted-foreground">
              Create your player profile before registering for an edition.
            </p>
          )}
        </AccountPanel>
        <AccountPanel
          actionHref="/account/registration"
          actionLabel="Registration"
          title="Tournament registration"
        >
          <p className="text-muted-foreground">
            {registrationStatusMessage(registration, Boolean(profile))}
          </p>
          {registration?.status ? (
            <Badge className="mt-3" variant="outline">
              {registration.status}
            </Badge>
          ) : null}
          <p className="mt-3 text-muted-foreground">
            {paymentStatusMessage(payments)}
          </p>
        </AccountPanel>
        <AccountPanel
          actionHref="/account/team"
          actionLabel="View team"
          title="My team"
        >
          {team ? (
            <>
              <p className="font-medium text-foreground">{team.teamName}</p>
              <p className="mt-1 text-muted-foreground">
                {team.me?.captain ? "Captain" : "Squad member"}
                {team.me?.jerseyNumber ? ` · #${team.me.jerseyNumber}` : ""}
              </p>
              <p className="mt-3 text-muted-foreground">
                {team.squad?.length ?? 0} active squad members
              </p>
            </>
          ) : (
            <p className="text-muted-foreground">
              Your approved registration has not been assigned to a team yet.
            </p>
          )}
        </AccountPanel>
        <AccountPanel
          actionHref="/account/matches"
          actionLabel="View matches"
          title="My matches"
        >
          {nextMatch ? (
            <>
              <p className="font-medium text-foreground">
                Match {nextMatch.matchNumber ?? "-"} ·{" "}
                {nextMatch.opponent?.name ?? "Opponent TBD"}
              </p>
              <p className="mt-1 text-muted-foreground">
                {nextMatch.status ?? "Scheduled"}
              </p>
              {nextMatchAction ? (
                <Link
                  className="mt-3 inline-flex text-sm font-semibold text-primary underline-offset-4 hover:underline"
                  href={nextMatchAction.href}
                >
                  {nextMatchAction.label}
                </Link>
              ) : null}
            </>
          ) : (
            <p className="text-muted-foreground">
              Your team fixtures will appear here after scheduling.
            </p>
          )}
        </AccountPanel>
        <AccountPanel
          actionHref="/account/statistics"
          actionLabel="View statistics"
          title="My statistics"
        >
          {statistics ? (
            <dl className="grid grid-cols-3 gap-3">
              <MiniStat label="Matches" value={statistics.matchesPlayed} />
              <MiniStat label="Runs" value={statistics.batting?.runs} />
              <MiniStat label="Wickets" value={statistics.bowling?.wickets} />
            </dl>
          ) : (
            <p className="text-muted-foreground">
              Edition statistics will appear once match data is available.
            </p>
          )}
        </AccountPanel>
      </section>
    </main>
  );
}

function AccountPanel({
  actionHref,
  actionLabel,
  children,
  title,
}: {
  actionHref: string;
  actionLabel: string;
  children: React.ReactNode;
  title: string;
}) {
  return (
    <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        {title}
      </h2>
      <div className="mt-4 min-h-20">{children}</div>
      <Link className={cn(buttonVariants(), "mt-5")} href={actionHref}>
        {actionLabel}
      </Link>
    </div>
  );
}

function MiniStat({ label, value }: { label: string; value?: number }) {
  return (
    <div>
      <dt className="text-xs uppercase text-muted-foreground">{label}</dt>
      <dd className="mt-1 text-lg font-semibold text-foreground">{value ?? 0}</dd>
    </div>
  );
}
