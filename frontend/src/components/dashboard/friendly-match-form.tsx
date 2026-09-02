"use client";

import { useMemo, useState } from "react";

import { ReviewSubmitButton } from "@/components/dashboard/review-submit-button";
import type { FriendlyPlayerOptionResponse, Venue } from "@/lib/api/schema-helpers";

type FriendlyMatchFormProps = {
  action: string;
  players: FriendlyPlayerOptionResponse[];
  venues: Venue[];
};

export function FriendlyMatchForm({
  action,
  players,
  venues,
}: FriendlyMatchFormProps) {
  const [teamAPlayers, setTeamAPlayers] = useState<number[]>([]);
  const [teamBPlayers, setTeamBPlayers] = useState<number[]>([]);
  const [query, setQuery] = useState("");
  const selected = new Set([...teamAPlayers, ...teamBPlayers]);
  const filteredPlayers = useMemo(() => {
    const normalized = query.trim().toLowerCase();

    return normalized
      ? players.filter((player) =>
          (player.fullName ?? "").toLowerCase().includes(normalized)
        )
      : players;
  }, [players, query]);

  return (
    <form action={action} className="grid gap-6" method="post">
      <section className="grid gap-4 rounded-sm border border-white/10 bg-card p-5 md:grid-cols-2">
        <TextField label="Team A name" name="teamAName" placeholder="Thunder XI" />
        <TextField label="Team B name" name="teamBName" placeholder="Warriors XI" />
        <label className="grid gap-2 text-sm">
          Overs
          <input
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            defaultValue={6}
            min={1}
            name="oversPerInnings"
            required
            type="number"
          />
        </label>
        <label className="grid gap-2 text-sm">
          Venue
          <select
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            disabled={venues.length === 0}
            name="venueId"
            required
          >
            <option value="">Select venue</option>
            {venues.map((venue) => (
              <option key={venue.id} value={venue.id}>
                {venue.name}
              </option>
            ))}
          </select>
        </label>
        <label className="grid gap-2 text-sm md:col-span-2">
          Scheduled time
          <input
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            name="scheduledAt"
            type="datetime-local"
          />
        </label>
      </section>
      <section className="rounded-sm border border-white/10 bg-card p-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
              Players
            </h2>
            <p className="mt-2 text-sm text-muted-foreground">
              Choose at least two players per side.
            </p>
          </div>
          <label className="grid min-w-60 gap-2 text-sm">
            Search
            <input
              className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Find player"
              type="search"
              value={query}
            />
          </label>
        </div>
        <div className="mt-5 grid gap-5 lg:grid-cols-2">
          <PlayerColumn
            name="teamAPlayerIds"
            players={filteredPlayers}
            selected={teamAPlayers}
            unavailable={new Set(teamBPlayers)}
            onChange={setTeamAPlayers}
            title="Team A"
          />
          <PlayerColumn
            name="teamBPlayerIds"
            players={filteredPlayers}
            selected={teamBPlayers}
            unavailable={new Set(teamAPlayers)}
            onChange={setTeamBPlayers}
            title="Team B"
          />
        </div>
        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-muted-foreground">
            {selected.size} unique player{selected.size === 1 ? "" : "s"} selected
          </p>
          <ReviewSubmitButton disabled={venues.length === 0}>
            Create friendly match
          </ReviewSubmitButton>
        </div>
      </section>
    </form>
  );
}

function TextField({
  label,
  name,
  placeholder,
}: {
  label: string;
  name: string;
  placeholder: string;
}) {
  return (
    <label className="grid gap-2 text-sm">
      {label}
      <input
        className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        maxLength={120}
        name={name}
        placeholder={placeholder}
        required
      />
    </label>
  );
}

function PlayerColumn({
  name,
  onChange,
  players,
  selected,
  title,
  unavailable,
}: {
  name: string;
  onChange: (value: number[]) => void;
  players: FriendlyPlayerOptionResponse[];
  selected: number[];
  title: string;
  unavailable: Set<number>;
}) {
  const selectedSet = new Set(selected);

  return (
    <div className="grid gap-3">
      <div className="flex items-center justify-between gap-3">
        <h3 className="font-heading text-lg font-bold uppercase tracking-normal">
          {title}
        </h3>
        <span className="font-mono text-xs uppercase text-muted-foreground">
          {selected.length} selected
        </span>
      </div>
      <div className="max-h-96 overflow-auto rounded-sm border border-white/10 bg-background p-2">
        {players.map((player) => {
          const playerId = player.playerId;
          const checked = playerId !== undefined && selectedSet.has(playerId);
          const disabled =
            playerId === undefined ||
            (!checked && unavailable.has(playerId));

          return (
            <label
              className="flex min-h-11 items-center gap-3 rounded-sm px-3 text-sm hover:bg-white/5"
              key={playerId ?? player.fullName}
            >
              <input
                checked={checked}
                disabled={disabled}
                name={name}
                onChange={(event) => {
                  if (playerId === undefined) {
                    return;
                  }

                  onChange(
                    event.target.checked
                      ? [...selected, playerId]
                      : selected.filter((id) => id !== playerId)
                  );
                }}
                type="checkbox"
                value={playerId}
              />
              <span>{player.fullName}</span>
              <span className="ml-auto text-xs text-muted-foreground">
                {player.primaryCategory ?? "Player"}
              </span>
            </label>
          );
        })}
      </div>
    </div>
  );
}
