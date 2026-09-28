import { Client } from "@stomp/stompjs";
import type { IMessage, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";

import {
  isNotificationSocketEvent,
  type NotificationSocketEvent,
} from "@/lib/api";

export const NOTIFICATIONS_DESTINATION = "/user/queue/notifications";
export const NOTIFICATION_ERRORS_DESTINATION =
  "/user/queue/notifications/errors";
export const MARK_READ_DESTINATION = "/app/notifications.read";
export const MARK_ALL_READ_DESTINATION = "/app/notifications.readAll";

/**
 * SockJS endpoint of the Spring broker. `NEXT_PUBLIC_WS_URL` wins when set;
 * otherwise the API origin is reused so the socket and REST always talk to the
 * same backend without extra configuration.
 */
export function resolveSocketUrl(): string {
  const configured = process.env.NEXT_PUBLIC_WS_URL?.trim();

  if (configured) {
    return configured.replace(/\/+$/, "");
  }

  const apiBase = (
    process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"
  ).replace(/\/+$/, "");

  return `${apiBase}/ws`;
}

export interface NotificationSocketHandlers {
  /** A notification event arrived on /user/queue/notifications. */
  onEvent: (event: NotificationSocketEvent) => void;
  /** The server refused a client action; message is safe to show. */
  onFailure: (message: string) => void;
  /**
   * Fired on every successful (re)connect. The server does not replay
   * notifications missed while offline, so listeners refetch over REST here.
   */
  onConnected: () => void;
  onDisconnected: () => void;
}

/**
 * Owns the lifetime of one STOMP-over-SockJS client for the notification
 * channel. One instance lives as long as the authenticated session.
 */
export class NotificationSocket {
  private client: Client | null = null;
  private subscription: StompSubscription | null = null;
  private errorSubscription: StompSubscription | null = null;
  private handlers: NotificationSocketHandlers | null = null;

  get isConnected(): boolean {
    return this.client?.connected ?? false;
  }

  connect(token: string, handlers: NotificationSocketHandlers): void {
    this.disconnect();
    this.handlers = handlers;

    const client = new Client({
      webSocketFactory: () =>
        new SockJS(resolveSocketUrl()) as unknown as WebSocket,
      connectHeaders: { Authorization: `Bearer ${token}` },
      // The broker drops sessions that go quiet, so keep the connection warm and
      // let stompjs recover from a server restart or a dropped network.
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      reconnectDelay: 5_000,
      debug: () => undefined,
      onConnect: () => {
        this.subscription = client.subscribe(
          NOTIFICATIONS_DESTINATION,
          (message) => this.handleNotificationFrame(message),
        );
        this.errorSubscription = client.subscribe(
          NOTIFICATION_ERRORS_DESTINATION,
          (message) => this.handleErrorFrame(message),
        );
        this.handlers?.onConnected();
      },
      onWebSocketClose: () => this.handlers?.onDisconnected(),
      onStompError: (frame) => {
        // A rejected CONNECT means the token is no longer accepted; surfacing it
        // lets the app fall back to REST instead of silently retrying forever.
        if (frame.headers.command === "CONNECT") {
          this.handlers?.onFailure(
            frame.headers.message ||
              "Live notifications are unavailable.",
          );
        }
      },
    });

    this.client = client;
    void client.activate();
  }

  disconnect(): void {
    this.subscription?.unsubscribe();
    this.errorSubscription?.unsubscribe();
    this.subscription = null;
    this.errorSubscription = null;
    this.handlers = null;

    const client = this.client;
    this.client = null;

    if (client) {
      void client.deactivate();
    }
  }

  /**
   * Publishes a client action when the socket is live. Returns false when the
   * caller must fall back to REST.
   */
  publish(destination: string, body: string): boolean {
    if (!this.client?.connected) {
      return false;
    }

    this.client.publish({ destination, body });
    return true;
  }

  private handleNotificationFrame(message: IMessage): void {
    try {
      const parsed: unknown = JSON.parse(message.body);

      if (isNotificationSocketEvent(parsed)) {
        this.handlers?.onEvent(parsed);
      }
    } catch {
      // A malformed frame must not tear down the subscription.
    }
  }

  private handleErrorFrame(message: IMessage): void {
    let text = "That notification action could not be completed.";

    try {
      const parsed = JSON.parse(message.body) as { message?: string };
      if (parsed?.message) {
        text = parsed.message;
      }
    } catch {
      // Keep the default wording when the body is not the expected JSON.
    }

    this.handlers?.onFailure(text);
  }
}
