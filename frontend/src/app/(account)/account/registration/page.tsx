import type { Metadata } from "next";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  getMyPayments,
  getMyProfile,
  getMyRegistration,
} from "@/lib/auth/account-api";
import {
  paymentStatusMessage,
  registrationStatusMessage,
  shouldOfferPayment,
} from "@/lib/auth/account-state";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

export const metadata: Metadata = {
  title: "Registration",
};

const categories = [
  { id: 1, label: "Batsman" },
  { id: 2, label: "Bowler" },
  { id: 3, label: "All-rounder" },
  { id: 4, label: "Wicketkeeper" },
];

const paymentMethods = ["CASH", "BKASH", "NAGAD", "BANK", "OTHER"] as const;

export default async function RegistrationPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const [params, currentEdition, profileResult] = await Promise.all([
    searchParams,
    getCurrentEditionData(),
    getMyProfile(),
  ]);
  const profile =
    profileResult && "ok" in profileResult && profileResult.ok
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
  const edition =
    currentEdition.status === "ready" ? currentEdition.edition : undefined;
  const registrationOpen = edition?.status === "REGISTRATION_OPEN";
  const canRegister = Boolean(profile && edition && registrationOpen && !registration);
  const fee = Number(registration?.feeAmount ?? edition?.registrationFee ?? 0);

  return (
    <main className="flex-1">
      <section className="border-b border-white/10 bg-background">
        <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <p className="font-mono text-xs uppercase text-primary">Account</p>
          <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
            Registration
          </h1>
          {edition ? (
            <p className="mt-3 text-sm text-muted-foreground">
              {edition.name} · {edition.status}
            </p>
          ) : null}
        </div>
      </section>
      <section className="mx-auto grid w-full max-w-5xl gap-4 px-4 py-8 sm:px-6 lg:px-8">
        {params.error ? (
          <p className="rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}
        <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
          <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
            Status
          </h2>
          <p className="mt-3 text-muted-foreground">
            {registrationStatusMessage(registration, Boolean(profile))}
          </p>
          {registration?.status ? (
            <Badge className="mt-3" variant="outline">
              {registration.status}
            </Badge>
          ) : null}
          {registration ? (
            <dl className="mt-5 grid gap-3 sm:grid-cols-2">
              <Info label="Category" value={registration.category} />
              <Info
                label="Fee"
                value={`${registration.feeAmount ?? 0} ${registration.currency ?? ""}`.trim()}
              />
              <Info label="Registered" value={registration.registeredAt} />
              <Info label="Registration ID" value={registration.id} />
            </dl>
          ) : null}
          {!profile ? (
            <Button className="mt-5" render={<Link href="/account/profile" />}>
              Create profile
            </Button>
          ) : null}
        </div>
        {canRegister ? (
          <form action="/api/account/registration" className="grid gap-4 rounded-sm border border-white/10 bg-card p-5 text-sm" method="post">
            <input name="editionId" type="hidden" value={edition?.id} />
            <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
              Register for this edition
            </h2>
            <label className="grid gap-2">
              Registration category
              <select
                className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                defaultValue={profile?.primaryCategory?.id ?? ""}
                name="categoryId"
                required
              >
                <option value="">Select category</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.label}
                  </option>
                ))}
              </select>
            </label>
            <Button className="h-10" type="submit">
              Submit registration
            </Button>
          </form>
        ) : null}
        {registration ? (
          <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
            <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
              Payments
            </h2>
            <p className="mt-3 text-muted-foreground">
              {paymentStatusMessage(payments)}
            </p>
            {payments.length > 0 ? (
              <div className="mt-5 grid gap-3">
                {payments.map((payment) => (
                  <div
                    className="rounded-sm border border-white/10 bg-background p-3"
                    key={payment.id}
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span>
                        {payment.amount} {registration.currency}
                      </span>
                      <Badge variant="outline">{payment.status}</Badge>
                    </div>
                    <p className="mt-2 text-muted-foreground">
                      {payment.paymentMethod}
                      {payment.transactionReference
                        ? ` · ${payment.transactionReference}`
                        : ""}
                    </p>
                  </div>
                ))}
              </div>
            ) : null}
            {shouldOfferPayment(registration, payments) ? (
              <form action="/api/account/payment" className="mt-5 grid gap-4" method="post">
                <input name="registrationId" type="hidden" value={registration.id} />
                <label className="grid gap-2">
                  Amount
                  <input
                    className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                    defaultValue={fee}
                    min="0.01"
                    name="amount"
                    required
                    step="0.01"
                    type="number"
                  />
                </label>
                <label className="grid gap-2">
                  Payment method
                  <select
                    className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                    name="paymentMethod"
                    required
                  >
                    {paymentMethods.map((method) => (
                      <option key={method} value={method}>
                        {method}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="grid gap-2">
                  Transaction reference
                  <input
                    className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                    name="transactionReference"
                  />
                </label>
                <Button className="h-10" type="submit">
                  Submit payment
                </Button>
              </form>
            ) : null}
          </div>
        ) : null}
      </section>
    </main>
  );
}

function Info({
  label,
  value,
}: {
  label: string;
  value?: number | string;
}) {
  return (
    <div>
      <dt className="text-xs uppercase text-muted-foreground">{label}</dt>
      <dd className="mt-1 font-medium text-foreground">{value ?? "Not available"}</dd>
    </div>
  );
}
