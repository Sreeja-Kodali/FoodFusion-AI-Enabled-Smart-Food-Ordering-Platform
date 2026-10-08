import { ApiError } from "./api";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? "" : "http://localhost:8084")).replace(/\/$/, "");

export interface AuthSession {
  accessToken: string;
  roles: string[];
}

export interface Credentials {
  username: string;
  email?: string;
  password: string;
}

export async function authenticate(mode: "login" | "register", credentials: Credentials): Promise<AuthSession> {
  const body = mode === "register"
    ? { email: credentials.email, displayName: credentials.username, password: credentials.password }
    : { email: credentials.username, password: credentials.password };
  const response = await fetch(`${apiBaseUrl}/api/auth/${mode}`, {
    method: "POST",
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    const detail = await response.text().catch(() => "");
    const fallback = response.status === 409
      ? "An account with those details already exists."
      : response.status === 401
        ? "Those sign-in details were not recognized."
        : response.status === 503
          ? "The account service is temporarily unavailable."
          : `The request failed (${response.status}).`;
    throw new ApiError(detail ? `${detail.slice(0, 180)}` : fallback, response.status);
  }

  const result = await response.json() as { accessToken?: unknown; roles?: unknown };
  if (typeof result.accessToken !== "string" || !result.accessToken) {
    throw new ApiError("The account service did not return an access token.");
  }
  return {
    accessToken: result.accessToken,
    roles: Array.isArray(result.roles)
      ? result.roles.filter((role): role is string => typeof role === "string")
      : [],
  };
}
