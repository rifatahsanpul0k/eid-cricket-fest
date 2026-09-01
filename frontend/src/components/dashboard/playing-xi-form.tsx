"use client";

import { useState } from "react";

import { ReviewSubmitButton } from "@/components/dashboard/review-submit-button";
import type { RosterCandidate } from "@/lib/dashboard/match-admin-state";
import { selectedCountLabel } from "@/lib/dashboard/match-admin-state";

export function PlayingXiForm({
  action,
  candidates,
  initialRegistrationIds = [],
  initialWicketkeeperRegistrationId,
  playingXiSize,
  returnTo,
  teamName,
  tournamentTeamId,
}: {
  action: string;
  candidates: RosterCandidate[];
  initialRegistrationIds?: number[];
  initialWicketkeeperRegistrationId?: number;
  playingXiSize: number;
  returnTo: string;
  teamName: string;
  tournamentTeamId: number;
}) {
  const [selected, setSelected] = useState<number[]>(initialRegistrationIds);
  const selectedSet = new Set(selected);
  const isFull = selected.length >= playingXiSize;

  return (
    <form action={action} className="grid gap-3" method="post">
      <input name="action" type="hidden" value="playing-xi" />
      <input name="returnTo" type="hidden" value={returnTo} />
      <input name="tournamentTeamId" type="hidden" value={tournamentTeamId} />
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-heading text-lg font-bold uppercase tracking-normal">
          {teamName}
        </h3>
        <span className="font-mono text-xs uppercase text-muted-foreground">
          {selectedCountLabel(selected.length, playingXiSize)}
        </span>
      </div>
      <div className="grid gap-2">
        {candidates.map((candidate) => {
          const checked = selectedSet.has(candidate.registrationId);

          return (
            <label
              className="flex min-h-11 items-center gap-3 rounded-sm border border-white/10 bg-background px-3 text-sm"
              key={candidate.registrationId}
            >
              <input
                checked={checked}
                disabled={!checked && isFull}
                name="registrationIds"
                onChange={(event) => {
                  setSelected((current) =>
                    event.target.checked
                      ? [...current, candidate.registrationId]
                      : current.filter((id) => id !== candidate.registrationId)
                  );
                }}
                type="checkbox"
                value={candidate.registrationId}
              />
              <span>{candidate.playerName}</span>
              <span className="ml-auto text-xs text-muted-foreground">
                #{candidate.registrationId}
              </span>
            </label>
          );
        })}
      </div>
      <label className="grid gap-2 text-sm">
        Wicketkeeper
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={initialWicketkeeperRegistrationId ?? ""}
          name="wicketkeeperRegistrationId"
        >
          <option value="">Not specified</option>
          {candidates.map((candidate) => (
            <option
              key={candidate.registrationId}
              value={candidate.registrationId}
            >
              {candidate.playerName}
            </option>
          ))}
        </select>
      </label>
      <ReviewSubmitButton>Submit XI</ReviewSubmitButton>
    </form>
  );
}
