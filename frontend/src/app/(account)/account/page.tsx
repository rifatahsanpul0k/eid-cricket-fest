import type { Metadata } from "next";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { getMyProfile, getMyRegistration, getMyPayments } from "@/lib/auth/account-api";
import {
  paymentStatusMessage,
  registrationStatusMessage,
} from "@/lib/auth/account-state";
import { getSession } from "@/lib/auth/session";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

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
  const payments = registration?.id ? await getMyPayments(registration.id) : [];

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
      <Button className="mt-5" render={<Link href={actionHref} />}>
        {actionLabel}
      </Button>
    </div>
  );
}
