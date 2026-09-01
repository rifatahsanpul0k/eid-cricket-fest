"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";

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
  batterOptions,
  buildByeDelivery,
  buildDeliveryRequest,
  buildLegByeDelivery,
  buildNoBallBatDelivery,
  buildNoBallByeDelivery,
  buildNoBallLegByeDelivery,
  buildWideDelivery,
  canCorrectDelivery,
  canUndo,
  currentBatters,
  deterministicDismissedBatter,
  deliveryLabel,
  dismissalOptionsForDelivery,
  dismissalRequiresFielder,
  eligibleActiveBatters,
  eligibleFielders,
  eligibleIncomingBatters,
  eligibleNextOverBowlers,
  extraRunOptions,
  needsBatters,
  needsBowler,
  nextBattingXi,
  nextBowlingXi,
  runOptions,
  validateDeliveryInput,
  wideOptions,
  xiIdForLivePlayer,
  type DismissalType,
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

export function ScorerConsole({
  initialState,
}: {
  initialState: ScorerMatchStateResponse;
}) {
  const [state, setState] = useState<ScorerMatchStateResponse>(initialState);
  const [connectionState, setConnectionState] =
    useState<LiveConnectionState>("connecting");
  const [busyKey, setBusyKey] = useState<string>();
  const [message, setMessage] = useState<string>();
  const [intentStore] = useState(() => new ScoringIntentStore());
  const stateRef = useRef(initialState);
  const liveRefreshKeyRef = useRef<string | undefined>(undefined);

  const matchId = state.match?.id;
  const innings = state.live?.innings;
  const scoringLocked =
    busyKey !== undefined ||
    connectionState === "disconnected" ||
    connectionState === "error";

  useEffect(() => {
    stateRef.current = state;
  }, [state]);

  useEffect(() => {
    if (!matchId) {
      return;
    }

    async function refreshScorerState(
      incoming: ScorerMatchStateResponse["live"]
    ) {
      if (!incoming?.matchId) {
        return;
      }

      const refreshKey = liveRefreshKey(incoming);

      if (refreshKey && liveRefreshKeyRef.current === refreshKey) {
        return;
      }

      liveRefreshKeyRef.current = refreshKey;

      const response = await fetch(`/api/scorer/matches/${incoming.matchId}`, {
        method: "GET",
      }).catch(() => undefined);

      const payload = response
        ? ((await response.json().catch(() => ({}))) as ServerPayload)
        : {};

      if (!payload.state) {
        return;
      }

      const incomingState = payload.state;

      setState((current) =>
        shouldAcceptScorerState(current, incomingState) ? incomingState : current
      );
    }

    const client = createLiveMatchClient({
      matchId,
      onConnectionState: setConnectionState,
      onUpdate: (incoming) => {
        if (!shouldAcceptLiveUpdate(stateRef.current.live, incoming)) {
          return;
        }

        setState((current) =>
          shouldAcceptLiveUpdate(current.live, incoming)
            ? { ...current, live: incoming }
            : current
        );
        void refreshScorerState(incoming);
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
                busy={busyKey}
                inningsId={innings?.inningsId}
                mutate={mutate}
                state={state}
              />
              <DeliveryPanel
                disabled={scoringLocked || needsBatters(state) || needsBowler(state)}
                onDelivery={submitDelivery}
                state={state}
              />
              <WicketPanel
                disabled={scoringLocked || needsBatters(state) || needsBowler(state)}
                onDelivery={submitDelivery}
                state={state}
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
  busy,
  inningsId,
  mutate,
  state,
}: {
  busy?: string;
  inningsId?: number;
  mutate: (body: MutationBody, actionKey: string) => void;
  state: ScorerMatchStateResponse;
}) {
  const battersNeeded = needsBatters(state);
  const bowlerNeeded = needsBowler(state);

  if (
    !inningsId ||
    state.match?.status !== "LIVE" ||
    (!battersNeeded && !bowlerNeeded)
  ) {
    return null;
  }

  return (
    <section className="grid gap-3 rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Change Players
      </h2>
      {battersNeeded ? (
        canUseIncomingBatterTransition(state) ? (
          <SetIncomingBatterForm
            batters={eligibleIncomingBatters(state)}
            disabled={Boolean(busy)}
            inningsId={inningsId}
            mutate={mutate}
            state={state}
          />
        ) : (
          <SetBattersForm
            batters={eligibleActiveBatters(state)}
            disabled={Boolean(busy)}
            inningsId={inningsId}
            mutate={mutate}
          />
        )
      ) : null}
      {bowlerNeeded ? (
        <SetBowlerForm
          bowlers={eligibleNextOverBowlers(state)}
          disabled={Boolean(busy)}
          inningsId={inningsId}
          mutate={mutate}
        />
      ) : null}
    </section>
  );
}

function DeliveryPanel({
  disabled,
  onDelivery,
  state,
}: {
  disabled: boolean;
  onDelivery: (actionKey: string, input: DeliveryInput) => void;
  state: ScorerMatchStateResponse;
}) {
  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Score Ball
      </h2>
      <div className="mt-4 grid grid-cols-3 gap-2">
        {runOptions().map((runs) => (
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
      <ExtrasForm disabled={disabled} onDelivery={onDelivery} state={state} />
    </section>
  );
}

function ExtrasForm({
  disabled,
  onDelivery,
  state,
}: {
  disabled: boolean;
  onDelivery: (actionKey: string, input: DeliveryInput) => void;
  state: ScorerMatchStateResponse;
}) {
  const [mode, setMode] = useState<
    "closed" | "wide" | "no-ball" | "bye" | "leg-bye" | "more"
  >("closed");
  const [otherRuns, setOtherRuns] = useState(1);
  const [extraWicketInput, setExtraWicketInput] = useState<DeliveryInput>();

  return (
    <div className="mt-4 grid gap-3">
      <div className="grid grid-cols-4 gap-2">
        {[
          ["WD", "wide"],
          ["NB", "no-ball"],
          ["B", "bye"],
          ["LB", "leg-bye"],
        ].map(([label, value]) => (
          <Button
            className="h-14 text-lg"
            disabled={disabled}
            key={value}
            onClick={() => {
              setExtraWicketInput(undefined);
              setMode(value as typeof mode);
            }}
            type="button"
            variant={mode === value ? "default" : "secondary"}
          >
            {label}
          </Button>
        ))}
      </div>
      {mode === "wide" ? (
        <div className="grid gap-3 rounded-sm border border-white/10 bg-background p-3">
          <p className="font-mono text-xs uppercase text-muted-foreground">
            Wide - total wide runs
          </p>
          <div className="grid grid-cols-3 gap-2">
            {wideOptions().map((runs) => (
              <Button
                className="h-14"
                disabled={disabled}
                key={runs}
                onClick={() =>
                  onDelivery(`delivery:wide:${runs}`, buildWideDelivery(runs))
                }
                type="button"
                variant="outline"
              >
                {runs}W
              </Button>
            ))}
          </div>
          <ExtraOtherRuns
            disabled={disabled}
            label="Other wide total"
            onRecord={(runs) =>
              onDelivery(`delivery:wide:${runs}`, buildWideDelivery(runs))
            }
            runs={otherRuns}
            setRuns={setOtherRuns}
          />
          <Button
            disabled={disabled}
            onClick={() => setExtraWicketInput(buildWideDelivery(1))}
            type="button"
            variant="destructive"
          >
            Wicket from Wide
          </Button>
        </div>
      ) : null}
      {mode === "no-ball" ? (
        <div className="grid gap-3 rounded-sm border border-white/10 bg-background p-3">
          <p className="font-mono text-xs uppercase text-muted-foreground">
            No Ball - +1 no-ball automatically
          </p>
          <div className="grid grid-cols-3 gap-2">
            {runOptions().map((runs) => (
              <Button
                className="h-14"
                disabled={disabled}
                key={runs}
                onClick={() =>
                  onDelivery(
                    `delivery:no-ball:bat:${runs}`,
                    buildNoBallBatDelivery(runs)
                  )
                }
                type="button"
                variant="outline"
              >
                NB + {runs}
              </Button>
            ))}
          </div>
          <div className="grid grid-cols-2 gap-2">
            <NoBallExtraGroup
              disabled={disabled}
              kind="bye"
              label="NB + Bye"
              onDelivery={onDelivery}
            />
            <NoBallExtraGroup
              disabled={disabled}
              kind="leg-bye"
              label="NB + Leg Bye"
              onDelivery={onDelivery}
            />
          </div>
          <Button
            disabled={disabled}
            onClick={() => setExtraWicketInput(buildNoBallBatDelivery(0))}
            type="button"
            variant="destructive"
          >
            Wicket from No Ball
          </Button>
        </div>
      ) : null}
      {mode === "bye" ? (
        <ExtraRunPanel
          disabled={disabled}
          label="Bye runs"
          onRecord={(runs) =>
            onDelivery(`delivery:bye:${runs}`, buildByeDelivery(runs))
          }
          runs={otherRuns}
          setRuns={setOtherRuns}
        />
      ) : null}
      {mode === "leg-bye" ? (
        <ExtraRunPanel
          disabled={disabled}
          label="Leg-bye runs"
          onRecord={(runs) =>
            onDelivery(`delivery:leg-bye:${runs}`, buildLegByeDelivery(runs))
          }
          runs={otherRuns}
          setRuns={setOtherRuns}
        />
      ) : null}
      <Button
        disabled={disabled}
        onClick={() => {
          setExtraWicketInput(undefined);
          setMode(mode === "more" ? "closed" : "more");
        }}
        type="button"
        variant="ghost"
      >
        More
      </Button>
      {mode === "more" ? (
        <div className="grid gap-2 rounded-sm border border-white/10 bg-background p-3">
          <ExtraOtherRuns
            disabled={disabled}
            label="Penalty runs"
            onRecord={(runs) =>
              onDelivery(`delivery:penalty:${runs}`, { penaltyRuns: runs })
            }
            runs={otherRuns}
            setRuns={setOtherRuns}
          />
        </div>
      ) : null}
      {extraWicketInput ? (
        <WicketFlow
          baseInput={extraWicketInput}
          disabled={disabled}
          onCancel={() => setExtraWicketInput(undefined)}
          onDelivery={onDelivery}
          state={state}
        />
      ) : null}
    </div>
  );
}

function ExtraRunPanel({
  disabled,
  label,
  onRecord,
  runs,
  setRuns,
}: {
  disabled: boolean;
  label: string;
  onRecord: (runs: number) => void;
  runs: number;
  setRuns: (runs: number) => void;
}) {
  return (
    <div className="grid gap-3 rounded-sm border border-white/10 bg-background p-3">
      <p className="font-mono text-xs uppercase text-muted-foreground">{label}</p>
      <div className="grid grid-cols-4 gap-2">
        {extraRunOptions().map((value) => (
          <Button
            className="h-14"
            disabled={disabled}
            key={value}
            onClick={() => onRecord(value)}
            type="button"
            variant="outline"
          >
            {value}
          </Button>
        ))}
      </div>
      <ExtraOtherRuns
        disabled={disabled}
        label="Other"
        onRecord={onRecord}
        runs={runs}
        setRuns={setRuns}
      />
    </div>
  );
}

function ExtraOtherRuns({
  disabled,
  label,
  onRecord,
  runs,
  setRuns,
}: {
  disabled: boolean;
  label: string;
  onRecord: (runs: number) => void;
  runs: number;
  setRuns: (runs: number) => void;
}) {
  return (
    <div className="grid grid-cols-[1fr_auto] gap-2">
      <label className="grid gap-1 text-xs uppercase text-muted-foreground">
        {label}
        <input
          className="min-h-12 rounded-sm border border-white/10 bg-background px-3 text-foreground"
          disabled={disabled}
          min={0}
          onChange={(event) => setRuns(Number(event.target.value))}
          type="number"
          value={runs}
        />
      </label>
      <Button
        className="self-end"
        disabled={disabled}
        onClick={() => onRecord(Math.max(0, runs))}
        type="button"
        variant="secondary"
      >
        Record
      </Button>
    </div>
  );
}

function NoBallExtraGroup({
  disabled,
  kind,
  label,
  onDelivery,
}: {
  disabled: boolean;
  kind: "bye" | "leg-bye";
  label: string;
  onDelivery: (actionKey: string, input: DeliveryInput) => void;
}) {
  return (
    <div className="grid gap-2">
      <p className="text-xs uppercase text-muted-foreground">{label}</p>
      <div className="grid grid-cols-2 gap-2">
        {extraRunOptions().map((runs) => {
          const input =
            kind === "bye"
              ? buildNoBallByeDelivery(runs)
              : buildNoBallLegByeDelivery(runs);

          return (
            <Button
              className="h-12"
              disabled={disabled}
              key={runs}
              onClick={() => onDelivery(`delivery:no-ball:${kind}:${runs}`, input)}
              type="button"
              variant="outline"
            >
              {runs}
            </Button>
          );
        })}
      </div>
    </div>
  );
}

function WicketPanel({
  disabled,
  onDelivery,
  state,
}: {
  disabled: boolean;
  onDelivery: (actionKey: string, input: DeliveryInput) => void;
  state: ScorerMatchStateResponse;
}) {
  const [open, setOpen] = useState(false);

  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
          Wicket
        </h2>
        <Button
          className="h-12"
          disabled={disabled}
          onClick={() => setOpen((value) => !value)}
          type="button"
          variant="destructive"
        >
          Wicket
        </Button>
      </div>
      {open ? (
        <div className="mt-3">
          <WicketFlow
            baseInput={{ runsOffBat: 0 }}
            disabled={disabled}
            onCancel={() => setOpen(false)}
            onDelivery={onDelivery}
            state={state}
          />
        </div>
      ) : null}
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
          <p className="flex min-h-11 items-center rounded-sm border border-white/10 bg-background px-3 text-sm text-muted-foreground">
            {correctionDeliveryId
              ? `Ball ${selectedCorrectionBall(state, correctionDeliveryId)?.sequence ?? "-"}`
              : latestBall?.deliveryId
                ? `Ball ${latestBall.sequence}`
                : "Tap a recent ball"}
          </p>
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
  const step =
    striker === ""
      ? "striker"
      : nonStriker === ""
        ? "non-striker"
        : bowler === ""
          ? "bowler"
          : "ready";
  const strikerOptions = batterOptions(batting, nonStriker);
  const nonStrikerOptions = batterOptions(batting, striker);
  const selectedStriker = batting.find((player) => player.playingXiId === striker);
  const selectedNonStriker = batting.find(
    (player) => player.playingXiId === nonStriker
  );
  const selectedBowler = bowling.find((player) => player.playingXiId === bowler);

  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Start Innings
      </h2>
      <div className="mt-3 grid gap-3">
        <SelectionSummary
          bowler={selectedBowler}
          nonStriker={selectedNonStriker}
          striker={selectedStriker}
        />
        {step === "striker" ? (
          <PlayerChoiceGrid
            disabled={disabled}
            label="Select striker"
            onSelect={(value) => {
              setStriker(value);

              if (value === nonStriker) {
                setNonStriker("");
              }
            }}
            players={strikerOptions}
            selectedId={striker}
          />
        ) : null}
        {step === "non-striker" ? (
          <PlayerChoiceGrid
            disabled={disabled}
            label="Select non-striker"
            onSelect={(value) => {
              setNonStriker(value);

              if (value === striker) {
                setStriker("");
              }
            }}
            players={nonStrikerOptions}
            selectedId={nonStriker}
          />
        ) : null}
        {step === "bowler" ? (
          <PlayerChoiceGrid
            disabled={disabled}
            label="Select opening bowler"
            onSelect={setBowler}
            players={bowling}
            selectedId={bowler}
          />
        ) : null}
        {step === "ready" ? (
          <div className="rounded-sm border border-white/10 bg-background p-3">
            <p className="font-mono text-xs uppercase text-muted-foreground">
              Ready to start
            </p>
          </div>
        ) : null}
        <div className="flex flex-wrap gap-2">
          {striker !== "" ? (
            <Button
              disabled={disabled}
              onClick={() => {
                setStriker("");
                setNonStriker("");
                setBowler("");
              }}
              type="button"
              variant="outline"
            >
              Change striker
            </Button>
          ) : null}
          {nonStriker !== "" ? (
            <Button
              disabled={disabled}
              onClick={() => {
                setNonStriker("");
                setBowler("");
              }}
              type="button"
              variant="outline"
            >
              Back
            </Button>
          ) : null}
          {bowler !== "" ? (
            <Button
              disabled={disabled}
              onClick={() => setBowler("")}
              type="button"
              variant="outline"
            >
              Change bowler
            </Button>
          ) : null}
        </div>
        <Button
          className="h-12"
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
  const strikerOptions = batterOptions(batters, nonStriker);
  const nonStrikerOptions = batterOptions(batters, striker);
  const selectedStriker = batters.find((player) => player.playingXiId === striker);
  const selectedNonStriker = batters.find(
    (player) => player.playingXiId === nonStriker
  );

  return (
    <div className="grid gap-3">
      <SelectionSummary
        nonStriker={selectedNonStriker}
        striker={selectedStriker}
      />
      {striker === "" ? (
        <PlayerChoiceGrid
          disabled={disabled}
          label="Select striker"
          onSelect={(value) => {
            setStriker(value);

            if (value === nonStriker) {
              setNonStriker("");
            }
          }}
          players={strikerOptions}
          selectedId={striker}
        />
      ) : null}
      {striker !== "" && nonStriker === "" ? (
        <PlayerChoiceGrid
          disabled={disabled}
          label="Select non-striker"
          onSelect={(value) => {
            setNonStriker(value);

            if (value === striker) {
              setStriker("");
            }
          }}
          players={nonStrikerOptions}
          selectedId={nonStriker}
        />
      ) : null}
      <div className="flex flex-wrap gap-2">
        {striker !== "" ? (
          <Button
            disabled={disabled}
            onClick={() => {
              setStriker("");
              setNonStriker("");
            }}
            type="button"
            variant="outline"
          >
            Change striker
          </Button>
        ) : null}
        {nonStriker !== "" ? (
          <Button
            disabled={disabled}
            onClick={() => setNonStriker("")}
            type="button"
            variant="outline"
          >
            Back
          </Button>
        ) : null}
      </div>
      <Button
        className="h-12"
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

function SetIncomingBatterForm({
  batters,
  disabled,
  inningsId,
  mutate,
  state,
}: {
  batters: ScorerPlayingXiPlayer[];
  disabled: boolean;
  inningsId: number;
  mutate: (body: MutationBody, actionKey: string) => void;
  state: ScorerMatchStateResponse;
}) {
  const pendingRef = useRef(false);

  useEffect(() => {
    if (!disabled) {
      pendingRef.current = false;
    }
  }, [disabled]);

  return (
    <div className="grid gap-2">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        Incoming batter
      </p>
      {batters.length === 0 ? (
        <p className="rounded-sm border border-white/10 bg-background p-3 text-sm text-muted-foreground">
          No eligible incoming batters available.
        </p>
      ) : null}
      {batters.map((player) => (
        <Button
          disabled={
            transitionButtonDisabled(
              disabled,
              false,
              player.playingXiId === undefined
            ) ||
            buildIncomingBatterMutation(inningsId, state, player) === undefined
          }
          key={player.playingXiId}
          onClick={() => {
            if (disabled || pendingRef.current) {
              return;
            }

            const transition =
              buildIncomingBatterMutation(inningsId, state, player);

            if (!transition) {
              return;
            }

            pendingRef.current = true;
            mutate(transition.body, transition.actionKey);
          }}
          type="button"
          variant="outline"
        >
          {player.playerName}
        </Button>
      ))}
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
  const pendingRef = useRef(false);

  useEffect(() => {
    if (!disabled) {
      pendingRef.current = false;
    }
  }, [disabled]);

  return (
    <div className="grid gap-2">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        Next bowler
      </p>
      {bowlers.length === 0 ? (
        <p className="rounded-sm border border-white/10 bg-background p-3 text-sm text-muted-foreground">
          No eligible bowlers available.
        </p>
      ) : null}
      {bowlers.map((player) => (
        <Button
          disabled={transitionButtonDisabled(
            disabled,
            false,
            player.playingXiId === undefined
          )}
          key={player.playingXiId}
          onClick={() => {
            if (
              disabled ||
              pendingRef.current ||
              player.playingXiId === undefined
            ) {
              return;
            }

            pendingRef.current = true;
            const transition = buildNextBowlerMutation(inningsId, player);

            if (transition) {
              mutate(transition.body, transition.actionKey);
            }
          }}
          type="button"
          variant="outline"
        >
          {player.playerName}
        </Button>
      ))}
    </div>
  );
}

function CreasePanel({
  state,
}: {
  state: ScorerMatchStateResponse;
}) {
  const innings = state.live?.innings;

  return (
    <section className="rounded-sm border border-white/10 bg-card p-4">
      <h2 className="font-heading text-xl font-bold uppercase tracking-normal">
        Crease
      </h2>
      <dl className="mt-3 grid gap-2 text-sm">
        <Detail label="Striker" value={innings?.striker?.name} marker="*" />
        <Detail label="Non-striker" value={innings?.nonStriker?.name} />
        <Detail label="Bowler" value={innings?.bowler?.name} />
        <Detail label="Bowling" value={innings?.bowlingTeam} />
      </dl>
    </section>
  );
}

function WicketFlow({
  baseInput,
  disabled,
  onCancel,
  onDelivery,
  state,
}: {
  baseInput: DeliveryInput;
  disabled: boolean;
  onCancel: () => void;
  onDelivery: (actionKey: string, input: DeliveryInput) => void;
  state: ScorerMatchStateResponse;
}) {
  const options = dismissalOptionsForDelivery(baseInput);
  const [dismissalType, setDismissalType] = useState<DismissalType>(
    options[0] ?? "BOWLED"
  );
  const [dismissedPlayingXiId, setDismissedPlayingXiId] =
    useState<PlayerSelectValue>("");
  const [fielderPlayingXiId, setFielderPlayingXiId] =
    useState<PlayerSelectValue>("");
  const [runOutRuns, setRunOutRuns] = useState(0);
  const deterministicDismissed = deterministicDismissedBatter(
    state,
    dismissalType
  );
  const finalDismissedPlayingXiId =
    deterministicDismissed ?? dismissedPlayingXiId;
  const fielders = eligibleFielders(state);
  const needsFielder = dismissalRequiresFielder(dismissalType);
  const needsDismissedChoice = deterministicDismissed === undefined;
  const selectedDismissed = activeBattingXi(state).find(
    (player) => player.playingXiId === finalDismissedPlayingXiId
  );
  const selectedFielder = fielders.find(
    (player) => player.playingXiId === fielderPlayingXiId
  );
  const canRecord =
    finalDismissedPlayingXiId !== "" &&
    (!needsFielder || fielderPlayingXiId !== "");

  return (
    <div className="grid gap-3 rounded-sm border border-white/10 bg-background p-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="font-mono text-xs uppercase text-muted-foreground">
            Wicket
          </p>
          <p className="mt-1 text-sm text-muted-foreground">
            {selectedDismissed?.playerName
              ? `Dismissed: ${selectedDismissed.playerName}`
              : "Choose dismissal details"}
          </p>
        </div>
        <Button disabled={disabled} onClick={onCancel} type="button" variant="ghost">
          Cancel
        </Button>
      </div>
      <DismissalChoiceGrid
        disabled={disabled}
        onSelect={(type) => {
          setDismissalType(type);
          setDismissedPlayingXiId("");
          setFielderPlayingXiId("");
        }}
        options={options}
        selected={dismissalType}
      />
      {needsDismissedChoice ? (
        <PlayerChoiceGrid
          disabled={disabled}
          label={
            dismissalType === "RUN_OUT"
              ? "Who is out?"
              : "Select dismissed batter"
          }
          onSelect={setDismissedPlayingXiId}
          players={currentBatters(state)}
          selectedId={dismissedPlayingXiId}
        />
      ) : null}
      {dismissalType === "RUN_OUT" ? (
        <div className="grid gap-2">
          <p className="font-mono text-xs uppercase text-muted-foreground">
            Runs completed
          </p>
          <div className="grid grid-cols-4 gap-2">
            {[0, 1, 2, 3].map((runs) => (
              <Button
                className="h-12"
                disabled={disabled}
                key={runs}
                onClick={() => setRunOutRuns(runs)}
                type="button"
                variant={runOutRuns === runs ? "default" : "outline"}
              >
                {runs}
              </Button>
            ))}
          </div>
        </div>
      ) : null}
      {needsFielder ? (
        <PlayerChoiceGrid
          disabled={disabled}
          label={
            dismissalType === "CAUGHT"
              ? "Who took the catch?"
              : "Responsible fielder"
          }
          onSelect={setFielderPlayingXiId}
          players={fielders}
          selectedId={fielderPlayingXiId}
        />
      ) : null}
      {selectedFielder ? (
        <p className="text-sm text-muted-foreground">
          Fielder: {selectedFielder.playerName}
        </p>
      ) : null}
      <Button
        className="h-12"
        disabled={disabled || !canRecord}
        onClick={() => {
          if (!canRecord) {
            return;
          }

          const input = buildWicketDeliveryInput(baseInput, runOutRuns);

          onDelivery(
            `delivery:wicket:${dismissalType}:${finalDismissedPlayingXiId}:${fielderPlayingXiId || "none"}:${runOutRuns}`,
            {
              ...input,
              wicket: {
                dismissalType,
                dismissedPlayingXiId: finalDismissedPlayingXiId,
                fielderPlayingXiId: fielderPlayingXiId || undefined,
              },
            }
          );
        }}
        type="button"
        variant="destructive"
      >
        Record Wicket
      </Button>
    </div>
  );
}

function buildWicketDeliveryInput(
  baseInput: DeliveryInput,
  runOutRuns: number
): DeliveryInput {
  if (runOutRuns <= 0) {
    return baseInput;
  }

  if ((baseInput.wideRuns ?? 0) > 0) {
    return {
      ...baseInput,
      wideRuns: Math.max(baseInput.wideRuns ?? 1, runOutRuns + 1),
    };
  }

  return {
    ...baseInput,
    runsOffBat: runOutRuns,
  };
}

function DismissalChoiceGrid({
  disabled,
  onSelect,
  options,
  selected,
}: {
  disabled: boolean;
  onSelect: (value: DismissalType) => void;
  options: DismissalType[];
  selected: DismissalType;
}) {
  return (
    <div className="grid gap-2">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        Dismissal
      </p>
      <div className="grid grid-cols-1 gap-2 min-[360px]:grid-cols-2">
        {options.map((type) => (
          <Button
            className="min-h-14 justify-start whitespace-normal px-3 py-2 text-left"
            disabled={disabled}
            key={type}
            onClick={() => onSelect(type)}
            type="button"
            variant={selected === type ? "default" : "outline"}
          >
            {type.replaceAll("_", " ")}
          </Button>
        ))}
      </div>
    </div>
  );
}

function PlayerChoiceGrid({
  disabled,
  label,
  onSelect,
  players,
  selectedId,
}: {
  disabled: boolean;
  label: string;
  onSelect: (value: number) => void;
  players: { playingXiId?: number; playerName?: string }[];
  selectedId: PlayerSelectValue;
}) {
  return (
    <div className="grid gap-2">
      <p className="font-mono text-xs uppercase text-muted-foreground">{label}</p>
      {players.length === 0 ? (
        <p className="rounded-sm border border-white/10 bg-card p-3 text-sm text-muted-foreground">
          No eligible players available.
        </p>
      ) : null}
      <div className="grid grid-cols-1 gap-2 min-[360px]:grid-cols-2">
        {players.map((player) => {
          const value = player.playingXiId;

          return (
            <Button
              className="min-h-14 justify-start whitespace-normal px-3 py-2 text-left"
              disabled={disabled || value === undefined}
              key={`${label}:${value}:${player.playerName}`}
              onClick={() => {
                if (value !== undefined) {
                  onSelect(value);
                }
              }}
              type="button"
              variant={selectedId === value ? "default" : "outline"}
            >
              {player.playerName ?? "Unnamed player"}
            </Button>
          );
        })}
      </div>
    </div>
  );
}

function SelectionSummary({
  bowler,
  nonStriker,
  striker,
}: {
  bowler?: ScorerPlayingXiPlayer;
  nonStriker?: ScorerPlayingXiPlayer;
  striker?: ScorerPlayingXiPlayer;
}) {
  return (
    <dl className="grid gap-2 rounded-sm border border-white/10 bg-background p-3 text-sm">
      <SummaryLine label="Striker" marker="*" value={striker?.playerName} />
      <SummaryLine label="Non-striker" value={nonStriker?.playerName} />
      {bowler ? <SummaryLine label="Bowler" value={bowler.playerName} /> : null}
    </dl>
  );
}

function SummaryLine({
  label,
  marker,
  value,
}: {
  label: string;
  marker?: string;
  value?: string;
}) {
  return (
    <div className="flex items-center justify-between gap-3">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="font-medium text-foreground">
        {marker ? `${marker} ` : ""}
        {value ?? "TBD"}
      </dd>
    </div>
  );
}

function selectedCorrectionBall(
  state: ScorerMatchStateResponse,
  deliveryId: number
) {
  return state.live?.recentBalls?.find((ball) => ball.deliveryId === deliveryId);
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
  marker,
  value,
}: {
  label: string;
  marker?: string;
  value?: string;
}) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-white/10 pb-2 last:border-b-0 last:pb-0">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="text-right">
        {marker ? `${marker} ` : ""}
        {value ?? "TBD"}
      </dd>
    </div>
  );
}

function liveRefreshKey(live: ScorerMatchStateResponse["live"]) {
  if (!live?.matchId) {
    return undefined;
  }

  return [
    live.matchId,
    live.innings?.inningsId ?? "none",
    live.innings?.scoreRevision ?? "unknown",
  ].join(":");
}

function shouldAcceptScorerState(
  current: ScorerMatchStateResponse,
  incoming: ScorerMatchStateResponse
) {
  const currentLive = current.live;
  const incomingLive = incoming.live;

  if (!currentLive || !incomingLive) {
    return true;
  }

  if (currentLive.matchId !== incomingLive.matchId) {
    return false;
  }

  const currentInnings = currentLive.innings;
  const incomingInnings = incomingLive.innings;

  if (!currentInnings || !incomingInnings) {
    return true;
  }

  if (currentInnings.inningsId !== incomingInnings.inningsId) {
    return true;
  }

  const currentRevision = currentInnings.scoreRevision;
  const incomingRevision = incomingInnings.scoreRevision;

  if (currentRevision === undefined || incomingRevision === undefined) {
    return true;
  }

  return incomingRevision >= currentRevision;
}

export function canUseIncomingBatterTransition(
  state: ScorerMatchStateResponse
) {
  return needsBatters(state) && currentBatters(state).length === 1;
}

export function buildIncomingBatterMutation(
  inningsId: number,
  state: ScorerMatchStateResponse,
  incoming: ScorerPlayingXiPlayer
) {
  const innings = state.live?.innings;
  const incomingXiId = incoming.playingXiId;

  if (!innings || incomingXiId === undefined) {
    return undefined;
  }

  const strikerXiId = xiIdForLivePlayer(
    activeBattingXi(state),
    innings.striker?.playerId
  );
  const nonStrikerXiId = xiIdForLivePlayer(
    activeBattingXi(state),
    innings.nonStriker?.playerId
  );

  if (strikerXiId !== undefined && nonStrikerXiId === undefined) {
    return {
      actionKey: `set-batter:${inningsId}:${incomingXiId}`,
      body: {
        action: "set-batters",
        inningsId,
        payload: {
          strikerPlayingXiId: strikerXiId,
          nonStrikerPlayingXiId: incomingXiId,
        },
      } satisfies MutationBody,
    };
  }

  if (strikerXiId === undefined && nonStrikerXiId !== undefined) {
    return {
      actionKey: `set-batter:${inningsId}:${incomingXiId}`,
      body: {
        action: "set-batters",
        inningsId,
        payload: {
          strikerPlayingXiId: incomingXiId,
          nonStrikerPlayingXiId: nonStrikerXiId,
        },
      } satisfies MutationBody,
    };
  }

  return undefined;
}

export function buildNextBowlerMutation(
  inningsId: number,
  bowler: ScorerPlayingXiPlayer
) {
  if (bowler.playingXiId === undefined) {
    return undefined;
  }

  return {
    actionKey: `set-bowler:${inningsId}:${bowler.playingXiId}`,
    body: {
      action: "set-bowler",
      inningsId,
      payload: {
        bowlerPlayingXiId: bowler.playingXiId,
      },
    } satisfies MutationBody,
  };
}

export function transitionButtonDisabled(
  disabled: boolean,
  pending: boolean,
  missingPlayerId: boolean
) {
  return disabled || pending || missingPlayerId;
}

function ScorerError({ message }: { message: string }) {
  return (
    <main className="grid min-h-[60vh] place-items-center px-4 text-center">
      <p className="text-sm text-muted-foreground">{message}</p>
    </main>
  );
}
