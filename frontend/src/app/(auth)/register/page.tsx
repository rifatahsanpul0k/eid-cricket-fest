import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";

import { Button } from "@/components/ui/button";
import { safeReturnTo } from "@/lib/auth/return-url";
import { getSession } from "@/lib/auth/session";

export const metadata: Metadata = {
  title: "Register",
};

export default async function RegisterPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; returnTo?: string }>;
}) {
  const session = await getSession();
  const params = await searchParams;
  const returnTo = safeReturnTo(params.returnTo);

  if (session) {
    redirect(returnTo);
  }

  return (
    <main className="mx-auto w-full max-w-md flex-1 px-4 py-12 sm:px-6">
      <div className="rounded-sm border border-white/10 bg-card p-6">
        <p className="font-mono text-xs uppercase text-primary">
          Player account
        </p>
        <h1 className="mt-3 font-heading text-3xl font-bold uppercase tracking-normal">
          Register
        </h1>
        {params.error ? (
          <p className="mt-4 rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}
        <form action="/api/auth/register" className="mt-6 grid gap-4" method="post">
          <input name="returnTo" type="hidden" value={returnTo} />
          <label className="grid gap-2 text-sm">
            Display name
            <input
              className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              name="displayName"
              required
            />
          </label>
          <label className="grid gap-2 text-sm">
            Email
            <input
              className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              name="email"
              type="email"
            />
          </label>
          <label className="grid gap-2 text-sm">
            Phone
            <input
              className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              name="phone"
            />
          </label>
          <label className="grid gap-2 text-sm">
            Password
            <input
              className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              maxLength={72}
              minLength={8}
              name="password"
              required
              type="password"
            />
          </label>
          <Button className="h-10" type="submit">
            Register
          </Button>
        </form>
        <p className="mt-4 text-sm text-muted-foreground">
          Already registered?{" "}
          <Link className="text-primary underline-offset-4 hover:underline" href="/login">
            Login
          </Link>
        </p>
      </div>
    </main>
  );
}
