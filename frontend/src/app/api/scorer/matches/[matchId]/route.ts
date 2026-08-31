import { NextRequest, NextResponse } from "next/server";

import type {
  CorrectDeliveryRequest,
  RecordDeliveryRequest,
  SetBattersRequest,
  SetBowlerRequest,
  StartInningsRequest,
  UndoDeliveryRequest,
} from "@/lib/api/schema-helpers";
import { problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";
import {
  correctScorerDelivery,
  getScorerMatchState,
  recordScorerDelivery,
  setScorerBatters,
  setScorerBowler,
  startScorerInnings,
  undoScorerDelivery,
} from "@/lib/scorer/scorer-api";

type ScorerMutationBody =
  | {
      action: "start-innings";
      payload: StartInningsRequest;
    }
  | {
      action: "set-batters";
      inningsId: number;
      payload: SetBattersRequest;
    }
  | {
      action: "set-bowler";
      inningsId: number;
      payload: SetBowlerRequest;
    }
  | {
      action: "delivery";
      inningsId: number;
      payload: RecordDeliveryRequest;
    }
  | {
      action: "undo";
      inningsId: number;
      payload: UndoDeliveryRequest;
    }
  | {
      action: "correction";
      deliveryId: number;
      payload: CorrectDeliveryRequest;
    };

export async function GET(
  _request: NextRequest,
  { params }: { params: Promise<{ matchId: string }> }
) {
  const matchId = parseMatchId((await params).matchId);

  if (!matchId) {
    return NextResponse.json({ error: "Invalid match id" }, { status: 400 });
  }

  const state = await getScorerMatchState(matchId);

  if (!state.ok) {
    return NextResponse.json(
      { error: problemMessage(state.error) ?? "Unable to load scorer match." },
      { status: state.status }
    );
  }

  return NextResponse.json({ state: state.data });
}

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ matchId: string }> }
) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const matchId = parseMatchId((await params).matchId);

  if (!matchId) {
    return NextResponse.json({ error: "Invalid match id" }, { status: 400 });
  }

  const body = (await request.json().catch(() => undefined)) as
    | ScorerMutationBody
    | undefined;

  if (!body) {
    return NextResponse.json({ error: "Invalid scorer request" }, { status: 400 });
  }

  const mutation = await runMutation(matchId, body);

  if (!mutation.ok) {
    const state = await getScorerMatchState(matchId);

    return NextResponse.json(
      {
        error: problemMessage(mutation.error) ?? "Scoring operation failed.",
        state: state.ok ? state.data : undefined,
      },
      { status: mutation.status }
    );
  }

  const state = await getScorerMatchState(matchId);

  if (!state.ok) {
    return NextResponse.json(
      {
        error: problemMessage(state.error) ?? "Scoring saved, but refresh failed.",
      },
      { status: state.status }
    );
  }

  return NextResponse.json({ state: state.data });
}

async function runMutation(matchId: number, body: ScorerMutationBody) {
  if (body.action === "start-innings") {
    return startScorerInnings(matchId, body.payload);
  }

  if (body.action === "set-batters") {
    return setScorerBatters(body.inningsId, body.payload);
  }

  if (body.action === "set-bowler") {
    return setScorerBowler(body.inningsId, body.payload);
  }

  if (body.action === "delivery") {
    return recordScorerDelivery(body.inningsId, body.payload);
  }

  if (body.action === "undo") {
    return undoScorerDelivery(body.inningsId, body.payload);
  }

  return correctScorerDelivery(body.deliveryId, body.payload);
}

function parseMatchId(value: string) {
  const parsed = Number.parseInt(value, 10);

  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}
