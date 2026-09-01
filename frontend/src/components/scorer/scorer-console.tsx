"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Button, buttonVariants } from "@/components/ui/button";
import type {
  CorrectDeliveryRequest,
  ScorerMatchStateResponse,
  ScorerPlayingXiPlayer,
} from "@/lib/api/schema-helpers";
import { shouldAcceptLiveUpdate, type LiveConnectionState } from "@/lib/realtime/live-update";
import { createLiveMatchClient } from "@/lib/realtime/stomp-client";
import {
  ScoringIntentStore,
  activeBattingXi,
  activeBowlingXi,
  buildDeliveryRequest,
  canCorrectDelivery,
  canUndo,
  deliveryLabel,
  needsBatters,
  needsBowler,
  nextBattingXi,
  nextBowlingXi,
  validateDeliveryInput,
  xiIdForLivePlayer,
  type DeliveryInput,
} from "@/lib/scorer/scorer-state";
import { cn } from "@/lib/utils";

type MutationBody = {
  action: string;
  inningsId?: number;
  deliveryId?: number;
  payload: unknown;
};

type ServerPayload = {
  error?: string;
  state?: ScorerMatchStateResponse;
};

type PlayerSelectValue = number | "";

const wicketTypes = [
  "BOWLED",
  "CAUGHT",
  "LBW",
  "RUN_OUT",
  "STUMPED",
  "HIT_WICKET",
  "HIT_BALL_TWICE",
  "OBSTRUCTING_FIELD",
] as const;

export function ScorerConsole({
  initialState,
}: {
  initialState: ScorerMatchStateResponse;
}) {
  const [state, setState] = useState(initialState);
  const [connectionState, setConnectionState] =
    useState<LiveConnectionState>("connecting");
  const [busyKey, setBusyKey] = useState<string>();
  const [message, setMessage] = useState<string>();
  const [intentStore] = useState(() => new ScoringIntentStore());

  const matchId = state.match?.id;
  const innings = state.live?.innings;
  const activeBatters = activeBattingXi(state);
  const activeBowlers = activeBowlingXi(state);
  const scoringLocked =
    busyKey !== undefined ||
    connectionState === "disconnected" ||
    connectionState === "error";

  useEffect(() => {
    if (!matchId) {
      return;
    }

    const client = createLiveMatchClient({
      matchId,
      onConnectionState: setConnectionState,
      onUpdate: (incoming) => {
        setState((current) =>
          shouldAcceptLiveUpdate(current.live, incoming)
            ? { ...current, live: incoming }
            : current
        );
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [matchId]);

  async function mutate(body: MutationBody, actionKey: string, keepIdOnFailure = false) {
    if (!matchId || busyKey) {
      return;
    }

    setBusyKey(actionKey);
    setMessage(undefined);

    try {
      const response = await fetch(`/api/scorer/matches/${matchId}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      });

      const payload = (await response.json().catch(() => ({}))) as ServerPayload;

      if (payload.state) {
        setState(payload.state);
      }

      if (!response.ok) {
        setMessage(payload.error ?? "Scoring operation failed.");

        if (!keepIdOnFailure || response.status < 500) {
          intentStore.finish(actionKey);
        }

        return;
      }

      intentStore.finish(actionKey);
    } catch {
      setMessage("Network uncertain. Retry will reuse the same event id.");
    } finally {
      setBusyKey(undefined);
    }
  }

  function submitDelivery(actionKey: string, input: DeliveryInput) {
    const validation = validateDeliveryInput(input);

    if (validation) {
      setMessage(validation);
      return;
    }

    const inningsId = innings?.inningsId;

    if (!inningsId) {
      setMessage("Start an innings before recording deliveries.");
      return;
    }

    const clientEventId = intentStore.begin(actionKey);

    return mutate(
      {
        action: "delivery",
        inningsId,
        payload: buildDeliveryRequest(clientEventId, input),
      },
      actionKey,
      true
    );
  }

  if (!matchId) {
    return <ScorerError message="Match state is unavailable." />;
  }

  const matchComplete = state.match?.status === "COMPLETED";

  return (
    <main className="flex-1 bg-background">
      <section className="sticky top-0 z-20 border-b border-white/10 bg-background/95 px-3 py-3 backdrop-blur">
        <div className="mx-auto flex w-full max-w-5xl items-start justify-between gap-3">
          <div>
            <p className="font-mono text-xs uppercase text-primary">
              Match {state.match?.matchNumber ?? "-"} · {state.match?.status ?? "-"}
            </p>
            <h1 className="mt-1 font-heading text-2xl font-bold uppercase tracking-normal">
              {state.match?.teamA?.name ?? "Team A"} vs{" "}
              {state.match?.teamB?.name ?? "Team B"}
            </h1>
          </div>
          <ConnectionBadge state={connectionState} />
        </div>
      </section>

      <section className="mx-auto grid w-full max-w-5xl gap-4 px-3 py-4 md:grid-cols-[1.15fr_0.85fr]">
        <div className="grid gap-4">
          <ScorePanel state={state} />
          {message ? (
            <p className="rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
              {message}
            </p>
          ) : null}
          {matchComplete ? (
            <CompletedPanel matchId={matchId} />
          ) : (
            <>
              <SetupPanel
                busy={busyKey}
                mutate={mutate}
                state={state}
              />
              <TransitionPanel
                activeBatters={activeBatters}
                activeBowlers={activeBowlers}
                busy={busyKey}
                inningsId={innings?.inningsId}
                mutate={mutate}
                state={state}
              />
              <DeliveryPanel
                disabled={scoringLocked || needsBatters(state) || needsBowler(state)}
                onDelivery={submitDelivery}
              />
              <WicketPanel
                batters={activeBatters}
                bowlers={activeBowlers}
                disabled={scoringLocked || needsBatters(state) || needsBowler(state)}
                onDelivery={submitDelivery}
              />
            </>
          )}
        </div>
        <aside className="grid content-start gap-4">
          <RecentPanel
            beginIntent={(actionKey) => intentStore.begin(actionKey)}
            busy={busyKey}
            mutate={mutate}
            state={state}
          />
          <CreasePanel
            batters={activeBatters}
            bowlers={activeBowlers}
            state={state}
          />
        </aside>
      </section>
    </main>
  );
}

function ScorePanel({ state }: { state: ScorerMatchStateResponse }) {
  const innings = state.live?.innings;

  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        {innings?.battingTeam ?? "Innings not started"}
      </p>
      <div className="mt-2 flex flex-wrap items-end gap-3">
        <p className="font-mono text-6xl font-bold tracking-normal text-secondary">
          {innings ? `${innings.runs ?? 0}/${innings.wickets ?? 0}` : "-/-"}
        </p>
        <p className="pb-2 font-mono text-sm uppercase text-muted-foreground">
          {innings?.overs ?? "0.0"} overs
        </p>
      </div>
      <dl className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-4">
        <Metric label="Target" value={innings?.target} />
        <Metric label="Need" value={innings?.runsRequired} />
        <Metric label="Balls" value={innings?.ballsRemaining} />
        <Metric label="Req RR" value={innings?.requiredRunRate} />
      </dl>
      <dl className="mt-2 grid grid-cols-2 gap-2">
        <Metric label="Run Rate" value={innings?.currentRunRate} />
        <Metric label="Revision" value={innings?.scoreRevision} />
      </dl>
    </section>
  );
}

function SetupPanel({
  busy,
  mutate,
  state,
}: {
  busy?: string;
  mutate: (body: MutationBody, actionKey: string) => void;
  state: ScorerMatchStateResponse;
}) {
  const batting = nextBattingXi(state);
  const bowling = nextBowlingXi(state);
  const canStart =
    (state.match?.status === "TOSS_COMPLETED" ||
      state.match?.status === "INNINGS_BREAK") &&
    batting.length > 1 &&
    bowling.length > 0 &&
    state.nextInningsBattingTeamId &&
    state.nextInningsBowlingTeamId;

  if (state.match?.status === "LIVE" || state.match?.status === "COMPLETED") {
    return null;
  }

  return (
    <StartInningsForm
      batting={batting}
      bowling={bowling}
      disabled={!canStart || Boolean(busy)}
      onStart={(payload) =>
        mutate({ action: "start-innings", payload }, "start-innings")
      }
    />
  );
}

function TransitionPanel({
  activeBatters,
  activeBowlers,
  busy,
  inningsId,
  mutate,
  state,
}: {
  activeBatters: ScorerPlayingXiPlayer[];
  activeBowlers: ScorerPlayingXiPlayer[];
  busy?: string;
  inningsId?: number;
  mutate: (body: MutationBody, actionKey: string) => void;
  state: ScorerMatchStateResponse;
}) {
  if (!inningsId || state.match?.status !== "LIVE") {
    return null;
  }

  return (
    <section className="grid gap-3 rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Change Players
      </h2>
      {needsBatters(state) ? (
        <SetBattersForm
          batters={activeBatters}
          disabled={Boolean(busy)}
          inningsId={inningsId}
          mutate={mutate}
        />
      ) : null}
      {needsBowler(state) ? (
        <SetBowlerForm
          bowlers={activeBowlers}
          disabled={Boolean(busy)}
          inningsId={inningsId}
          mutate={mutate}
        />
      ) : null}
      {!needsBatters(state) && !needsBowler(state) ? (
        <div className="grid gap-3 sm:grid-cols-2">
          <SetBattersForm
            batters={activeBatters}
            disabled={Boolean(busy)}
            inningsId={inningsId}
            mutate={mutate}
          />
          <SetBowlerForm
            bowlers={activeBowlers}
            disabled={Boolean(busy)}
            inningsId={inningsId}
            mutate={mutate}
          />
        </div>
      ) : null}
    </section>
  );
}

function DeliveryPanel({
  disabled,
  onDelivery,
}: {
  disabled: boolean;
  onDelivery: (actionKey: string, input: DeliveryInput) => void;
}) {
  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Score Ball
      </h2>
      <div className="mt-4 grid grid-cols-3 gap-2">
        {[0, 1, 2, 3, 4, 6].map((runs) => (
          <Button
            className="h-16 text-2xl"
            disabled={disabled}
            key={runs}
            onClick={() => onDelivery(`delivery:runs:${runs}`, { runsOffBat: runs })}
            type="button"
          >
            {runs}
          </Button>
        ))}
      </div>
      <ExtrasForm disabled={disabled} onDelivery={onDelivery} />
    </section>
  );
}

function ExtrasForm({
  disabled,
  onDelivery,
}: {
  disabled: boolean;
  onDelivery: (actionKey: string, input: DeliveryInput) => void;
}) {
  const [kind, setKind] = useState("wide");
  const [runs, setRuns] = useState(1);

  return (
    <div className="mt-4 grid gap-3">
      <div className="grid grid-cols-[1fr_96px] gap-2">
        <select
          className="min-h-12 rounded-sm border border-white/10 bg-background px-3"
          disabled={disabled}
          onChange={(event) => setKind(event.target.value)}
          value={kind}
        >
          <option value="wide">Wide</option>
          <option value="no-ball">No-ball</option>
          <option value="bye">Bye</option>
          <option value="leg-bye">Leg-bye</option>
          <option value="penalty">Penalty</option>
        </select>
        <input
          className="min-h-12 rounded-sm border border-white/10 bg-background px-3"
          disabled={disabled}
          min={0}
          onChange={(event) => setRuns(Number(event.target.value))}
          type="number"
          value={runs}
        />
      </div>
      <Button
        disabled={disabled}
        onClick={() => {
          const value = Math.max(0, runs);
          const input =
            kind === "wide"
              ? { wideRuns: Math.max(1, value) }
              : kind === "no-ball"
                ? { noBallRuns: Math.max(1, value) }
                : kind === "bye"
                  ? { byeRuns: value }
                  : kind === "leg-bye"
                    ? { legByeRuns: value }
                    : { penaltyRuns: value };

          onDelivery(`delivery:${kind}:${value}`, input);
        }}
        type="button"
        variant="secondary"
      >
        Record Extra
      </Button>
    </div>
  );
}

function WicketPanel({
  batters,
  bowlers,
  disabled,
  onDelivery,
}: {
  batters: ScorerPlayingXiPlayer[];
  bowlers: ScorerPlayingXiPlayer[];
  disabled: boolean;
  onDelivery: (actionKey: string, input: DeliveryInput) => void;
}) {
  const [dismissedPlayingXiId, setDismissedPlayingXiId] =
    useState<PlayerSelectValue>("");
  const [dismissalType, setDismissalType] =
    useState<(typeof wicketTypes)[number]>("BOWLED");
  const [fielderPlayingXiId, setFielderPlayingXiId] =
    useState<PlayerSelectValue>("");

  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Wicket
      </h2>
      <div className="mt-3 grid gap-2">
        <PlayerSelect
          disabled={disabled}
          label="Dismissed"
          onChange={setDismissedPlayingXiId}
          players={batters}
          value={dismissedPlayingXiId}
        />
        <label className="grid gap-1 text-sm">
          Dismissal
          <select
            className="min-h-12 rounded-sm border border-white/10 bg-background px-3"
            disabled={disabled}
            onChange={(event) =>
              setDismissalType(event.target.value as (typeof wicketTypes)[number])
            }
            value={dismissalType}
          >
            {wicketTypes.map((type) => (
              <option key={type} value={type}>
                {type.replaceAll("_", " ")}
              </option>
            ))}
          </select>
        </label>
        <PlayerSelect
          allowEmpty
          disabled={disabled}
          label="Fielder"
          onChange={setFielderPlayingXiId}
          players={bowlers}
          value={fielderPlayingXiId}
        />
        <Button
          disabled={disabled || dismissedPlayingXiId === ""}
          onClick={() =>
            onDelivery(`delivery:wicket:${dismissalType}:${dismissedPlayingXiId}`, {
              wicket: {
                dismissalType,
                dismissedPlayingXiId: dismissedPlayingXiId || 0,
                fielderPlayingXiId: fielderPlayingXiId || undefined,
              },
            })
          }
          type="button"
          variant="destructive"
        >
          Record Wicket
        </Button>
      </div>
    </section>
  );
}

function RecentPanel({
  beginIntent,
  busy,
  mutate,
  state,
}: {
  beginIntent: (actionKey: string) => string;
  busy?: string;
  mutate: (body: MutationBody, actionKey: string, keepIdOnFailure?: boolean) => void;
  state: ScorerMatchStateResponse;
}) {
  const inningsId = state.live?.innings?.inningsId;
  const latestBall = state.live?.recentBalls?.at(-1);
  const [correctionDeliveryId, setCorrectionDeliveryId] = useState<number>();
  const [correctionRuns, setCorrectionRuns] = useState(0);
  const [reason, setReason] = useState("Scorer correction");

  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Recent
      </h2>
      <div className="mt-3 flex flex-wrap gap-2">
        {(state.live?.recentBalls ?? []).map((ball) => (
          <button
            className="grid size-11 place-items-center rounded-full bg-surface-elevated font-mono font-bold"
            key={ball.deliveryId ?? ball.sequence}
            onClick={() => {
              setCorrectionDeliveryId(ball.deliveryId);
              setCorrectionRuns(ball.runs ?? 0);
            }}
            title={ball.commentary}
            type="button"
          >
            {deliveryLabel(ball)}
          </button>
        ))}
      </div>
      <div className="mt-4 grid gap-2">
        <Button
          disabled={!inningsId || !canUndo(state) || Boolean(busy)}
          onClick={() => {
            if (!inningsId) {
              return;
            }

            const key = "undo:last";
            const clientEventId = beginIntent(key);

            mutate(
              {
                action: "undo",
                inningsId,
                payload: {
                  clientEventId,
                  reason: "Undo last ball",
                },
              },
              key,
              true
            );
          }}
          type="button"
          variant="outline"
        >
          Undo Last Ball
        </Button>
        <label className="grid gap-1 text-sm">
          Correction reason
          <input
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3"
            onChange={(event) => setReason(event.target.value)}
            value={reason}
          />
        </label>
        <div className="grid grid-cols-[1fr_96px] gap-2">
          <select
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3"
            disabled={!canCorrectDelivery(state)}
            onChange={(event) =>
              setCorrectionDeliveryId(Number(event.target.value) || undefined)
            }
            value={correctionDeliveryId ?? latestBall?.deliveryId ?? ""}
          >
            <option value="">Select ball</option>
            {(state.live?.recentBalls ?? []).map((ball) => (
              <option key={ball.deliveryId} value={ball.deliveryId}>
                Ball {ball.sequence}: {deliveryLabel(ball)}
              </option>
            ))}
          </select>
          <input
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3"
            min={0}
            onChange={(event) => setCorrectionRuns(Number(event.target.value))}
            type="number"
            value={correctionRuns}
          />
        </div>
        <Button
          disabled={!canCorrectDelivery(state) || Boolean(busy)}
          onClick={() => {
            const deliveryId = correctionDeliveryId ?? latestBall?.deliveryId;

            if (!deliveryId) {
              return;
            }

            const key = `correction:${deliveryId}:${correctionRuns}`;
            const clientEventId = beginIntent(key);
            const payload: CorrectDeliveryRequest = {
              ...buildDeliveryRequest(clientEventId, { runsOffBat: correctionRuns }),
              reason,
            };

            mutate(
              {
                action: "correction",
                deliveryId,
                payload,
              },
              key,
              true
            );
          }}
          type="button"
          variant="secondary"
        >
          Submit Correction
        </Button>
      </div>
    </section>
  );
}

function StartInningsForm({
  batting,
  bowling,
  disabled,
  onStart,
}: {
  batting: ScorerPlayingXiPlayer[];
  bowling: ScorerPlayingXiPlayer[];
  disabled: boolean;
  onStart: (payload: {
    strikerPlayingXiId: number;
    nonStrikerPlayingXiId: number;
    bowlerPlayingXiId: number;
  }) => void;
}) {
  const [striker, setStriker] = useState<PlayerSelectValue>("");
  const [nonStriker, setNonStriker] = useState<PlayerSelectValue>("");
  const [bowler, setBowler] = useState<PlayerSelectValue>("");

  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Start Innings
      </h2>
      <div className="mt-3 grid gap-2">
        <PlayerSelect
          disabled={disabled}
          label="Striker"
          onChange={setStriker}
          players={batting}
          value={striker}
        />
        <PlayerSelect
          disabled={disabled}
          label="Non-striker"
          onChange={setNonStriker}
          players={batting}
          value={nonStriker}
        />
        <PlayerSelect
          disabled={disabled}
          label="Bowler"
          onChange={setBowler}
          players={bowling}
          value={bowler}
        />
        <Button
          disabled={
            disabled ||
            striker === "" ||
            nonStriker === "" ||
            bowler === "" ||
            striker === nonStriker
          }
          onClick={() =>
            onStart({
              strikerPlayingXiId: striker || 0,
              nonStrikerPlayingXiId: nonStriker || 0,
              bowlerPlayingXiId: bowler || 0,
            })
          }
          type="button"
        >
          Start Innings
        </Button>
      </div>
    </section>
  );
}

function SetBattersForm({
  batters,
  disabled,
  inningsId,
  mutate,
}: {
  batters: ScorerPlayingXiPlayer[];
  disabled: boolean;
  inningsId: number;
  mutate: (body: MutationBody, actionKey: string) => void;
}) {
  const [striker, setStriker] = useState<PlayerSelectValue>("");
  const [nonStriker, setNonStriker] = useState<PlayerSelectValue>("");

  return (
    <div className="grid gap-2">
      <PlayerSelect
        disabled={disabled}
        label="Striker"
        onChange={setStriker}
        players={batters}
        value={striker}
      />
      <PlayerSelect
        disabled={disabled}
        label="Non-striker"
        onChange={setNonStriker}
        players={batters}
        value={nonStriker}
      />
      <Button
        disabled={
          disabled ||
          striker === "" ||
          nonStriker === "" ||
          striker === nonStriker
        }
        onClick={() =>
          mutate(
            {
              action: "set-batters",
              inningsId,
              payload: {
                strikerPlayingXiId: striker || 0,
                nonStrikerPlayingXiId: nonStriker || 0,
              },
            },
            "set-batters"
          )
        }
        type="button"
        variant="outline"
      >
        Set Batters
      </Button>
    </div>
  );
}

function SetBowlerForm({
  bowlers,
  disabled,
  inningsId,
  mutate,
}: {
  bowlers: ScorerPlayingXiPlayer[];
  disabled: boolean;
  inningsId: number;
  mutate: (body: MutationBody, actionKey: string) => void;
}) {
  const [bowler, setBowler] = useState<PlayerSelectValue>("");

  return (
    <div className="grid gap-2">
      <PlayerSelect
        disabled={disabled}
        label="Bowler"
        onChange={setBowler}
        players={bowlers}
        value={bowler}
      />
      <Button
        disabled={disabled || bowler === ""}
        onClick={() =>
          mutate(
            {
              action: "set-bowler",
              inningsId,
              payload: {
                bowlerPlayingXiId: bowler || 0,
              },
            },
            "set-bowler"
          )
        }
        type="button"
        variant="outline"
      >
        Set Bowler
      </Button>
    </div>
  );
}

function CreasePanel({
  batters,
  bowlers,
  state,
}: {
  batters: ScorerPlayingXiPlayer[];
  bowlers: ScorerPlayingXiPlayer[];
  state: ScorerMatchStateResponse;
}) {
  const innings = state.live?.innings;
  const strikerXiId = xiIdForLivePlayer(batters, innings?.striker?.playerId);
  const nonStrikerXiId = xiIdForLivePlayer(
    batters,
    innings?.nonStriker?.playerId
  );
  const bowlerXiId = xiIdForLivePlayer(bowlers, innings?.bowler?.playerId);

  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Crease
      </h2>
      <dl className="mt-3 grid gap-2 text-sm">
        <Detail label="Striker" value={innings?.striker?.name} meta={strikerXiId} />
        <Detail label="Non-striker" value={innings?.nonStriker?.name} meta={nonStrikerXiId} />
        <Detail label="Bowler" value={innings?.bowler?.name} meta={bowlerXiId} />
        <Detail label="Bowling" value={innings?.bowlingTeam} />
      </dl>
    </section>
  );
}

function CompletedPanel({ matchId }: { matchId: number }) {
  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Match Complete
      </h2>
      <Link
        className={cn(buttonVariants({ variant: "secondary" }), "mt-4")}
        href={`/matches/${matchId}/scorecard`}
      >
        Open Scorecard
      </Link>
    </section>
  );
}

function PlayerSelect({
  allowEmpty,
  disabled,
  label,
  onChange,
  players,
  value,
}: {
  allowEmpty?: boolean;
  disabled: boolean;
  label: string;
  onChange: (value: PlayerSelectValue) => void;
  players: ScorerPlayingXiPlayer[];
  value: PlayerSelectValue;
}) {
  return (
    <label className="grid gap-1 text-sm">
      {label}
      <select
        className="min-h-12 rounded-sm border border-white/10 bg-background px-3"
        disabled={disabled}
        onChange={(event) => onChange(Number(event.target.value) || "")}
        value={value}
      >
        <option value="">{allowEmpty ? "None" : "Choose player"}</option>
        {players.map((player) => (
          <option key={player.playingXiId} value={player.playingXiId}>
            {player.playerName}
          </option>
        ))}
      </select>
    </label>
  );
}

function ConnectionBadge({ state }: { state: LiveConnectionState }) {
  const label =
    state === "connected"
      ? "Connected"
      : state === "connecting"
        ? "Connecting"
        : state === "reconnecting"
          ? "Reconnecting"
          : state === "error"
            ? "Connection error"
            : "Disconnected";

  return (
    <span className="rounded-full border border-white/10 bg-surface-elevated px-3 py-2 font-mono text-xs uppercase text-muted-foreground">
      {label}
    </span>
  );
}

function Metric({
  label,
  value,
}: {
  label: string;
  value?: number;
}) {
  return (
    <div className="rounded-sm border border-white/10 bg-surface p-3">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-1 font-mono text-lg font-bold">{value ?? "-"}</dd>
    </div>
  );
}

function Detail({
  label,
  meta,
  value,
}: {
  label: string;
  meta?: number;
  value?: string;
}) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-white/10 pb-2 last:border-b-0 last:pb-0">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="text-right">
        {value ?? "TBD"}
        {meta ? (
          <span className="ml-2 font-mono text-xs text-muted-foreground">
            XI {meta}
          </span>
        ) : null}
      </dd>
    </div>
  );
}

function ScorerError({ message }: { message: string }) {
  return (
    <main className="grid min-h-[60vh] place-items-center px-4 text-center">
      <p className="text-sm text-muted-foreground">{message}</p>
    </main>
  );
}
