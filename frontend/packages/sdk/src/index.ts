import type { AdminWorkbenchSummary, ApiResponse, MallEntryView } from "@gmall/shared-types";

const DEFAULT_GATEWAY_BASE_URL = "http://127.0.0.1:18070";

function gatewayBaseUrl(): string {
  return (globalThis as unknown as { __GMALL_GATEWAY__?: string }).__GMALL_GATEWAY__ ?? DEFAULT_GATEWAY_BASE_URL;
}

async function request<T>(path: string, init?: RequestInit): Promise<ApiResponse<T>> {
  const response = await fetch(`${gatewayBaseUrl()}${path}`, init);
  return response.json() as Promise<ApiResponse<T>>;
}

export function resolveMallEntry(entryType: string): Promise<ApiResponse<MallEntryView>> {
  const params = new URLSearchParams({
    entryType,
    terminalType: "MOBILE",
    locale: "zh-CN"
  });
  return request<MallEntryView>(`/api/mall/entries/resolve?${params.toString()}`);
}

export function getAdminWorkbenchSummary(): Promise<ApiResponse<AdminWorkbenchSummary>> {
  return request<AdminWorkbenchSummary>("/api/admin/workbench/summary");
}

export function getMallCart(): Promise<ApiResponse<Record<string, unknown>>> {
  return request<Record<string, unknown>>("/api/mall/cart?locale=zh-CN");
}
