import Link from "next/link";

import { ReviewSubmitButton } from "@/components/dashboard/review-submit-button";
import { buttonVariants } from "@/components/ui/button";
import type {
  AwardPlayerOption,
  AwardType,
  PlayerAward,
} from "@/lib/api/awards";
import type { TournamentEdition } from "@/lib/api/tournaments";
import { formatAwardType } from "@/lib/cricket/formatters";
import { cn } from "@/lib/utils";

export type TournamentFinalizationAwardData = {
  awards: PlayerAward[];
  options: AwardPlayerOption[];
};

export function TournamentFinalization({
  awardData,
  edition,
}: {
  awardData?: TournamentFinalizationAwardData;
  edition: TournamentEdition;
}) {
  return (
    <>
      <LifecycleStrip
        awardCount={awardData?.awards.length ?? 0}
        edition={edition}
      />
      <CompletionSummary edition={edition} />
      {edition.id && edition.status === "COMPLETED" ? (
        <AwardsManagement
          awards={awardData?.awards ?? []}
          editionId={edition.id}
          options={awardData?.options ?? []}
        />
      ) : null}
    </>
  );
}

function LifecycleStrip({
  awardCount,
  edition,
}: {
  awardCount: number;
  edition: TournamentEdition;
}) {
  const steps = lifecycleSteps(edition, awardCount);

  return (
    <section className="grid gap-3 border-y border-white/10 py-4 sm:grid-cols-2 lg:grid-cols-7">
      {steps.map((step) => (
        <div className="grid gap-1" key={step.label}>
          <dt className="font-mono text-xs uppercase text-muted-foreground">
            {step.label}
          </dt>
          <dd className={cn("text-sm font-medium", step.complete && "text-secondary")}>
            {step.complete ? "Complete" : "Pending"}
          </dd>
        </div>
      ))}
    </section>
  );
}

function CompletionSummary({
  edition,
}: {
  edition: TournamentEdition;
}) {
  if (edition.status !== "COMPLETED") {
    return null;
  }

  return (
    <section className="grid gap-3 rounded-sm border border-secondary/30 bg-secondary/10 p-4 text-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-mono text-xs uppercase text-secondary">
            Tournament Complete
          </p>
          <h3 className="mt-2 font-heading text-2xl font-bold uppercase tracking-normal">
            {edition.champion?.name ?? "Champion pending"}
          </h3>
        </div>
        {edition.finalMatchId ? (
          <Link
            className={cn(buttonVariants({ variant: "outline" }))}
            href={`/dashboard/matches/${edition.finalMatchId}`}
          >
            Final match
          </Link>
        ) : null}
      </div>
      <dl className="grid gap-3 sm:grid-cols-3">
        <SummaryDetail label="Champion" value={edition.champion?.name ?? "TBD"} />
        <SummaryDetail label="Runner-up" value={edition.runnerUp?.name ?? "TBD"} />
        <SummaryDetail
          label="Completed"
          value={formatDateTime(edition.completedAt)}
        />
      </dl>
    </section>
  );
}

function AwardsManagement({
  awards,
  editionId,
  options,
}: {
  awards: PlayerAward[];
  editionId: number;
  options: AwardPlayerOption[];
}) {
  return (
    <section className="grid gap-4 rounded-sm border border-white/10 bg-background p-4">
      <div>
        <p className="font-mono text-xs uppercase text-muted-foreground">
          Awards
        </p>
        <h3 className="mt-2 font-heading text-xl font-bold uppercase tracking-normal">
          Manage Awards
        </h3>
      </div>

      {awards.length ? (
        <ul className="grid gap-2 text-sm">
          {awards.map((award) => (
            <li
              className="flex flex-col gap-1 rounded-sm border border-white/10 p-3 sm:flex-row sm:items-center sm:justify-between"
              key={award.id}
            >
              <span>{award.title ?? formatAwardType(award.awardType)}</span>
              <span className="text-muted-foreground">
                {award.playerName}
                {award.teamName ? `, ${award.teamName}` : ""}
              </span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-sm text-muted-foreground">
          No awards have been assigned yet.
        </p>
      )}

      <AwardForm editionId={editionId} options={options} />
    </section>
  );
}

function AwardForm({
  editionId,
  options,
}: {
  editionId: number;
  options: AwardPlayerOption[];
}) {
  return (
    <form
      action="/api/dashboard/tournament"
      className="grid gap-4 border-t border-white/10 pt-4 text-sm"
      method="post"
    >
      <input name="action" type="hidden" value="assign-award" />
      <input name="editionId" type="hidden" value={editionId} />
      <input name="returnTo" type="hidden" value="/dashboard/tournament" />
      <div className="grid gap-4 md:grid-cols-2">
        <SelectField label="Award type" name="awardType" required>
          {awardTypes.map((awardType) => (
            <option key={awardType} value={awardType}>
              {formatAwardType(awardType)}
            </option>
          ))}
        </SelectField>
        <SelectField label="Recipient" name="registrationId" required>
          <option value="">Select player</option>
          {options.map((option) => (
            <option key={option.registrationId} value={option.registrationId}>
              {option.playerName}
              {option.teamName ? `, ${option.teamName}` : ""}
            </option>
          ))}
        </SelectField>
        <TextField label="Custom title" maxLength={150} name="title" />
        <label className="grid gap-2 md:col-span-2">
          Notes
          <textarea
            className="min-h-24 rounded-sm border border-white/10 bg-background px-3 py-2 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            maxLength={1000}
            name="notes"
          />
        </label>
      </div>
      <ReviewSubmitButton disabled={!options.length}>
        Assign award
      </ReviewSubmitButton>
    </form>
  );
}

function SelectField({
  children,
  label,
  name,
  required,
}: {
  children: React.ReactNode;
  label: string;
  name: string;
  required?: boolean;
}) {
  return (
    <label className="grid gap-2">
      {label}
      <select
        className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        name={name}
        required={required}
      >
        {children}
      </select>
    </label>
  );
}

function TextField({
  label,
  maxLength,
  name,
}: {
  label: string;
  maxLength?: number;
  name: string;
}) {
  return (
    <label className="grid gap-2">
      {label}
      <input
        className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        maxLength={maxLength}
        name={name}
      />
    </label>
  );
}

function SummaryDetail({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="rounded-sm border border-white/10 bg-background p-3">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-2 font-medium">{value}</dd>
    </div>
  );
}

const awardTypes: AwardType[] = [
  "PLAYER_OF_TOURNAMENT",
  "FINAL_MVP",
  "BEST_FIELDER",
  "EMERGING_PLAYER",
  "CUSTOM",
];

function lifecycleSteps(edition: TournamentEdition, awardCount: number) {
  const statusOrder = [
    "REGISTRATION_CLOSED",
    "DRAFTING",
    "SCHEDULED",
    "ONGOING",
    "COMPLETED",
  ];
  const currentIndex = statusOrder.indexOf(edition.status ?? "DRAFT");

  return [
    {
      complete: currentIndex >= 0,
      label: "Registration",
    },
    {
      complete: currentIndex >= 1,
      label: "Draft",
    },
    {
      complete: currentIndex >= 2,
      label: "League",
    },
    {
      complete: currentIndex >= 3,
      label: "Knockout",
    },
    {
      complete: Boolean(edition.finalMatchId),
      label: "Final",
    },
    {
      complete: awardCount > 0,
      label: "Awards",
    },
    {
      complete: edition.status === "COMPLETED",
      label: "Completed",
    },
  ];
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString() : "Not set";
}
