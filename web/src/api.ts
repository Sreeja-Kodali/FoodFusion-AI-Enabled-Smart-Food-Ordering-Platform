import type { AuthSession } from "./auth";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? "" : "http://localhost:8084")).replace(/\/$/, "");

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
}

export interface OrderReceipt {
  id: string;
  placedAt: string;
  items: Array<{ name: string; quantity: number }>;
  total: number;
  status: string;
}

export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

export interface Notification {
  id: string;
  orderNumber: string;
  status: string;
  message: string;
  totalAmount: number;
  createdAt: string;
}

export interface DailyAnalytics {
  date: string;
  orderCount: number;
  revenue: number;
}

export interface AnalyticsSummary {
  orderCount: number;
  revenue: number;
}

export class ApiError extends Error {
  constructor(message: string, readonly status?: number) {
    super(message);
    this.name = "ApiError";
  }
}

async function request(
  path: string,
  session?: AuthSession | null,
  options: RequestInit = {},
): Promise<Response> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (session) headers.set("Authorization", `Bearer ${session.accessToken}`);
  const response = await fetch(`${apiBaseUrl}${path}`, { ...options, headers });
  if (response.status === 401) throw new ApiError("Please sign in again to continue.", 401);
  return response;
}

async function errorFromResponse(response: Response, action: string): Promise<ApiError> {
  const detail = await response.text().catch(() => "");
  const statusHint = response.status === 503
    ? "A connected service is unavailable. Please try again shortly."
    : response.status === 404
      ? "The requested resource was not found."
      : response.status === 403
        ? "This account is not allowed to perform that action."
        : `The request failed (${response.status}).`;
  return new ApiError(`${action}: ${detail ? detail.slice(0, 180) : statusHint}`, response.status);
}

function productFrom(value: unknown): Product | null {
  if (!value || typeof value !== "object") return null;
  const item = value as Record<string, unknown>;
  const id = item.id ?? item.foodId ?? item.skuCode;
  if (id == null) return null;
  return {
    id: String(id),
    name: String(item.name ?? item.foodName ?? "Unnamed dish"),
    description: String(item.description ?? ""),
    price: Number(item.price ?? item.cost ?? 0),
  };
}

export async function searchFoods(query: string, session?: AuthSession | null): Promise<Product[]> {
  const response = await request(`/api/foods/search?q=${encodeURIComponent(query)}`, session);
  if (!response.ok) throw await errorFromResponse(response, "Could not search the menu");
  const result: unknown = await response.json();
  const records = Array.isArray(result)
    ? result
    : result && typeof result === "object"
      ? ((result as Record<string, unknown>).foods ?? (result as Record<string, unknown>).content ?? (result as Record<string, unknown>).results)
      : null;
  if (!Array.isArray(records)) throw new ApiError("The menu response was not in the expected format.");
  return records.map(productFrom).filter((product): product is Product => product !== null);
}

function orderFrom(value: unknown): OrderReceipt | null {
  if (!value || typeof value !== "object") return null;
  const order = value as Record<string, unknown>;
  const id = order.id ?? order.orderId ?? order.orderNumber;
  if (id == null) return null;
  const rawItems = order.items ?? order.orderItems ?? order.orderLineItems ?? order.orderLineItemsDTOs;
  const items = Array.isArray(rawItems) ? rawItems.map((entry) => {
    const line = entry as Record<string, unknown>;
    return {
      name: String(line.name ?? line.foodName ?? line.skuCode ?? "Food item"),
      quantity: Number(line.quantity ?? 1),
    };
  }) : [];
  return {
    id: String(id),
    placedAt: String(order.placedAt ?? order.createdAt ?? order.orderDate ?? ""),
    items,
    total: Number(order.total ?? order.totalAmount ?? order.amount ?? 0),
    status: String(order.status ?? order.orderStatus ?? order.state ?? "Received"),
  };
}

export async function getOrderHistory(session: AuthSession): Promise<OrderReceipt[]> {
  const response = await request("/api/order/history", session);
  if (!response.ok) throw await errorFromResponse(response, "Could not load order history");
  const result: unknown = await response.json();
  const records = Array.isArray(result)
    ? result
    : result && typeof result === "object"
      ? ((result as Record<string, unknown>).orders ?? (result as Record<string, unknown>).content)
      : null;
  if (!Array.isArray(records)) throw new ApiError("Order history was not returned in the expected format.");
  return records.map(orderFrom).filter((order): order is OrderReceipt => order !== null);
}

export async function getNotifications(session: AuthSession): Promise<Notification[]> {
  const response = await request("/api/notifications", session);
  if (!response.ok) throw await errorFromResponse(response, "Could not load notifications");
  const result: unknown = await response.json();
  if (!Array.isArray(result)) throw new ApiError("Notifications were not returned in the expected format.");
  return result.filter((value): value is Record<string, unknown> => Boolean(value) && typeof value === "object")
    .map((notification) => ({
      id: String(notification.id ?? ""),
      orderNumber: String(notification.orderNumber ?? ""),
      status: String(notification.status ?? ""),
      message: String(notification.message ?? ""),
      totalAmount: Number(notification.totalAmount ?? 0),
      createdAt: String(notification.createdAt ?? ""),
    }))
    .filter((notification) => notification.id && notification.orderNumber);
}

export async function getAnalyticsSummary(session: AuthSession): Promise<{
  summary: AnalyticsSummary;
  daily: DailyAnalytics[];
}> {
  const [summaryResponse, dailyResponse] = await Promise.all([
    request("/api/analytics/summary", session),
    request("/api/analytics/daily", session),
  ]);
  if (!summaryResponse.ok) throw await errorFromResponse(summaryResponse, "Could not load analytics summary");
  if (!dailyResponse.ok) throw await errorFromResponse(dailyResponse, "Could not load daily analytics");
  const summary: unknown = await summaryResponse.json();
  const daily: unknown = await dailyResponse.json();
  if (!summary || typeof summary !== "object" || !Array.isArray(daily)) {
    throw new ApiError("Analytics were not returned in the expected format.");
  }
  const values = summary as Record<string, unknown>;
  return {
    summary: {
      orderCount: Number(values.orderCount ?? 0),
      revenue: Number(values.revenue ?? 0),
    },
    daily: daily.filter((value): value is Record<string, unknown> => Boolean(value) && typeof value === "object")
      .map((record) => ({
        date: String(record.date ?? ""),
        orderCount: Number(record.orderCount ?? 0),
        revenue: Number(record.revenue ?? 0),
      }))
      .filter((record) => record.date),
  };
}

export async function placeOrder(
  items: Array<{ id: string; price: number; quantity: number }>,
  session: AuthSession,
): Promise<void> {
  const response = await request("/api/order", session, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      orderLineItemsDTOs: items.map((item) => ({
        skuCode: item.id,
        price: item.price,
        quantity: item.quantity,
      })),
    }),
  });
  if (!response.ok) throw await errorFromResponse(response, "Could not place your order");
}

export async function sendChatMessage(message: string, session: AuthSession): Promise<string> {
  const response = await request("/api/ai/chat", session, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message }),
  });
  if (!response.ok) throw await errorFromResponse(response, "The food assistant is unavailable");
  const result: unknown = await response.json().catch(() => null);
  if (typeof result === "string") return result;
  if (result && typeof result === "object") {
    const payload = result as Record<string, unknown>;
    const content = payload.response ?? payload.reply ?? payload.message ?? payload.content;
    if (typeof content === "string") return content;
  }
  throw new ApiError("The assistant response was not in the expected format.");
}
