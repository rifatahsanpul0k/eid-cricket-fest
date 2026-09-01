import type { Metadata } from "next";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { getMyProfile, getPlayerCategories } from "@/lib/auth/account-api";

export const metadata: Metadata = {
  title: "Profile",
};

export default async function ProfilePage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string }>;
}) {
  const [params, profileResult, categories] = await Promise.all([
    searchParams,
    getMyProfile(),
    getPlayerCategories(),
  ]);
  const profile =
    profileResult && "ok" in profileResult && profileResult.ok
      ? profileResult.data
      : undefined;

  return (
    <main className="flex-1">
      <section className="border-b border-white/10 bg-background">
        <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <p className="font-mono text-xs uppercase text-primary">Account</p>
          <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
            Profile
          </h1>
        </div>
      </section>
      <section className="mx-auto w-full max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
        {params.error ? (
          <p className="mb-4 rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}
        {profile ? (
          <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
            <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
              {profile.fullName}
            </h2>
            <div className="mt-4 flex flex-wrap gap-2">
              {profile.primaryCategory?.name ? (
                <Badge variant="outline">{profile.primaryCategory.name}</Badge>
              ) : null}
              {profile.battingStyle ? (
                <Badge variant="secondary">{profile.battingStyle}</Badge>
              ) : null}
              {profile.bowlingStyle ? (
                <Badge variant="secondary">{profile.bowlingStyle}</Badge>
              ) : null}
            </div>
            {profile.dateOfBirth ? (
              <p className="mt-4 text-muted-foreground">
                Date of birth: {profile.dateOfBirth}
              </p>
            ) : null}
          </div>
        ) : (
          <form action="/api/account/profile" className="grid gap-4 rounded-sm border border-white/10 bg-card p-5 text-sm" method="post">
            <label className="grid gap-2">
              Full name
              <input
                className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                name="fullName"
                required
              />
            </label>
            <label className="grid gap-2">
              Primary category
              <select
                className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                disabled={categories.length === 0}
                name="primaryCategoryId"
              >
                <option value="">Select category</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
              {categories.length === 0 ? (
                <span className="text-xs text-muted-foreground">
                  No active player categories are available.
                </span>
              ) : null}
            </label>
            <label className="grid gap-2">
              Date of birth
              <input
                className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                name="dateOfBirth"
                type="date"
              />
            </label>
            <label className="grid gap-2">
              Batting style
              <input
                className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                name="battingStyle"
              />
            </label>
            <label className="grid gap-2">
              Bowling style
              <input
                className="h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                name="bowlingStyle"
              />
            </label>
            <Button className="h-10" type="submit">
              Create profile
            </Button>
          </form>
        )}
      </section>
    </main>
  );
}
