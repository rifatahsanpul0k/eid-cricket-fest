import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import type { TournamentEdition } from "@/lib/api/tournaments";
import { getSession } from "@/lib/auth/session";
import { cn } from "@/lib/utils";

export async function RegistrationCta({
  edition,
}: {
  edition: TournamentEdition;
}) {
  const session = await getSession();
  const registrationOpen = edition.status === "REGISTRATION_OPEN";
  const authenticated = Boolean(session);
  const actions = authenticated
    ? [
        {
          href: "/account",
          label: "My Account",
          variant: "outline" as const,
        },
        {
          href: "/account/registration",
          label: "Tournament Registration",
          variant: "secondary" as const,
        },
      ]
    : [
        {
          href: "/login",
          label: "Login",
          variant: "outline" as const,
        },
        {
          href: "/register",
          label: "Register",
          variant: "secondary" as const,
        },
      ];

  return (
    <section className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-8" id="registration">
      <div className="rounded-sm border border-white/10 bg-card p-6 md:flex md:items-center md:justify-between md:gap-6">
        <div>
          <p className="font-mono text-xs font-medium uppercase text-primary">
            {registrationOpen ? "Registration Open" : "Player Portal"}
          </p>
          <h2 className="mt-1 font-heading text-3xl font-bold uppercase tracking-normal">
            {registrationOpen
              ? "Register for the current edition"
              : "Player access for this edition"}
          </h2>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
            Registration status is synced from the tournament API. Player
            account access will appear here when the selected edition is ready.
          </p>
        </div>
        <div className="mt-5 flex flex-col gap-2 sm:flex-row md:mt-0">
          {actions.map((action) => (
            <Link
              className={cn(
                buttonVariants({ variant: action.variant }),
                "h-auto rounded-sm px-4 py-3 text-center text-sm font-semibold",
                action.variant === "outline"
                  ? "border-white/20 text-muted-foreground"
                  : "text-secondary-foreground"
              )}
              href={action.href}
              key={action.href}
            >
              {action.label}
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}
