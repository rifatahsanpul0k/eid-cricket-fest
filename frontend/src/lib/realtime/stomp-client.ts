"use client";

import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";

import type { LiveMatch } from "@/lib/api/matches";
import { getWebSocketUrl } from "@/lib/realtime/env";
import {
  liveMatchTopic,
  parseLiveUpdate,
  type LiveConnectionState,
} from "@/lib/realtime/live-update";

export function createLiveMatchClient({
  matchId,
  onConnectionState,
  onUpdate,
}: {
  matchId: number;
  onConnectionState: (state: LiveConnectionState) => void;
  onUpdate: (update: LiveMatch) => void;
}) {
  let subscription: StompSubscription | undefined;
  let hasConnected = false;

  const client = new Client({
    brokerURL: getWebSocketUrl(),
    reconnectDelay: 5_000,
    debug: () => {},
    beforeConnect: () => {
      onConnectionState(hasConnected ? "reconnecting" : "connecting");
    },
    onConnect: () => {
      hasConnected = true;
      onConnectionState("connected");
      subscription?.unsubscribe();
      subscription = client.subscribe(liveMatchTopic(matchId), (message: IMessage) => {
        const update = parseLiveUpdate(message.body);

        if (update) {
          onUpdate(update);
        }
      });
    },
    onDisconnect: () => {
      onConnectionState("disconnected");
    },
    onStompError: () => {
      onConnectionState("error");
    },
    onWebSocketClose: () => {
      onConnectionState(client.active ? "reconnecting" : "disconnected");
    },
    onWebSocketError: () => {
      onConnectionState("error");
    },
  });

  return {
    activate() {
      onConnectionState("connecting");
      client.activate();
    },
    deactivate() {
      subscription?.unsubscribe();
      subscription = undefined;
      onConnectionState("disconnected");
      void client.deactivate();
    },
  };
}
